use std::{
    io::{BufRead, Error, ErrorKind},
    net::TcpStream,
    rc::Rc,
    sync::mpsc,
    thread,
};

use otrr::{instancetag::InstanceTag, Policy, UserMessage};
use protocol::read_message;

use crate::protocol::write_message;

mod client;
mod protocol;

fn main() {
    let mut conn = TcpStream::connect("127.0.0.1:8080")
        .expect("Failed to open socket connection to echoserver.");
    let (interop_sender, interop_receiver) = mpsc::sync_channel::<InteropMessage>(250);
    let input_sender = interop_sender.clone();

    thread::spawn(move || {
        let mut input = std::io::stdin().lock();
        loop {
            let cmd = read_command(&mut input);
            if cmd.is_err() {
                dbg!(cmd.unwrap_err());
                continue;
            }
            input_sender.send(InteropMessage::Send(cmd.unwrap())).expect("Interop-channel unexpectedly closed.");
        }
    });

    let mut conn_receive = conn.try_clone().unwrap();
    thread::spawn(move || loop {
        let data = read_message(&mut conn_receive);
        if data.is_err() {
            continue;
        }
        let data = data.unwrap();
        interop_sender.send(InteropMessage::Receive(data)).expect("Interop-channel unexpectedly closed.");
    });

    let mut account = otrr::session::Account::new(
        Rc::new(client::Client::new(conn.try_clone().unwrap())),
        Policy::ALLOW_V3 | Policy::ALLOW_V4 | Policy::WHITESPACE_START_AKE | Policy::ERROR_START_AKE,
    );
    loop {
        match interop_receiver.recv().unwrap() {
            InteropMessage::Receive(msg) => {
                let result = account.session(&msg.0).receive(&msg.1);
                if result.is_err() {
                    todo!("Handle error.");
                }
                handle(result.unwrap());
            }
            // FIXME must check sending on particular outgoing session (instance tag)
            InteropMessage::Send(msg) => {
                let result = account.session(&msg.0).send(msg.1, &msg.2);
                for part in result.unwrap() {
                    write_message(&mut conn, &msg.0, &part).unwrap();
                }
            }
        }
    }
}

enum InteropMessage {
    Send((Vec<u8>, InstanceTag, Vec<u8>)),
    Receive((Vec<u8>, Vec<u8>)),
}

fn handle(msg: UserMessage) -> Option<(InstanceTag, Vec<u8>)> {
    println!("Waiting to receive message…");
    // FIXME convert to string
    match msg {
        UserMessage::None => {
            println!("Message processed without result for user.");
            None
        }
        UserMessage::Error(err) => {
            println!("Error occurred: {:?}", err);
            None
        }
        UserMessage::Plaintext(content) => {
            println!(
                "Plaintext message: {:?}",
                String::from_utf8(content.clone()).unwrap()
            );
            Some((0, content))
        }
        UserMessage::WarningUnencrypted(content) => {
            println!("Received unencrypted message: {:?}", String::from_utf8(content.clone()).unwrap());
            // FIXME add prefix message about "received this unencrypted: ..."
            Some((0, content))
        }
        UserMessage::Confidential(tag, content, tlvs) => {
            println!(
                "Confidential message on instance {:?} with {} tlvs: {:?}",
                tag,
                tlvs.len(),
                String::from_utf8(content.clone()).unwrap(),
            );
            Some((tag, content))
        }
        UserMessage::ConfidentialSessionStarted(tag) => {
            println!("Confidential session started on {}", tag);
            None
        }
        UserMessage::ConfidentialSessionFinished(tag, content) => {
            println!(
                "Confidential session finished on {}: {:?}",
                tag,
                String::from_utf8(content).unwrap()
            );
            None
        }
        UserMessage::Reset(tag) => {
            println!("State-reset to plaintext for {}", tag);
            None
        }
        UserMessage::SMPSucceeded(tag) => {
            println!("Successfully completed authentication (SMP) on {}", tag);
            None
        }
        UserMessage::SMPFailed(tag) => {
            println!("Failed authentication (SMP) on {}", tag);
            None
        }
    }
}

fn read_command(input: &mut dyn BufRead) -> Result<(Vec<u8>, InstanceTag, Vec<u8>), Error> {
    let mut line = String::new();
    input.read_line(&mut line)?;
    let (addresscell, message) = line.split_once(' ').ok_or(Error::from(ErrorKind::InvalidInput))?;
    if let Some((address, tagvalue)) = addresscell.split_once('#') {
        let tag = InstanceTag::from_str_radix(tagvalue, 10u32).or(Err(Error::from(ErrorKind::InvalidInput)))?;
        Ok((Vec::from(address), tag, Vec::from(message)))
    } else {
        Ok((Vec::from(addresscell), 0, Vec::from(message)))
    }
}

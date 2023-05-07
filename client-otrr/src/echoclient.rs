mod client;
mod protocol;

extern crate dsa;
extern crate otrr;

use otrr::{Policy, UserMessage};
use std::{cell::RefCell, net::TcpStream, rc::Rc};

use protocol::write_message;

fn main() {
    let stream = Rc::new(RefCell::new(
        TcpStream::connect("127.0.0.1:8080")
            .expect("Failed to open socket connection to echoserver."),
    ));
    let client = Rc::new(client::Client::new(Rc::clone(&stream)));
    let mut account = otrr::session::Account::new(
        client,
        Policy::ALLOW_V3 | Policy::WHITESPACE_START_AKE | Policy::ERROR_START_AKE,
    );
    loop {
        println!("Waiting to receive message…");
        let msg = protocol::read_message(&mut (*stream.as_ref().borrow_mut()))
            .expect("Failed to read message from stream.");
        let result = account.session(&msg.0).receive(&msg.1);
        if let Err(err) = result {
            println!("Error processing received message: {:?}", err);
            continue;
        }
        // FIXME convert to string
        let followup = match result.unwrap() {
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
                println!("Received unencrypted message: {:?}", content);
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
                    String::from_utf8(content.clone()).unwrap()
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
        };
        let (tag, response) = match followup {
            None => continue,
            Some(response) => response,
        };
        match account.session(&msg.0).send(tag, &response) {
            Err(error) => println!("{:?}", error),
            Ok(parts) => {
                for part in parts {
                    write_message(&mut (*stream.as_ref().borrow_mut()), &msg.0, &part)
                        .expect("Failed to transmit message part.");
                }
            }
        }
    }
}

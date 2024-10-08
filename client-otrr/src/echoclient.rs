mod client;
mod protocol;

extern crate dsa;
extern crate otrr;

use otrr::{Policy, UserMessage};
use std::{net::TcpStream, rc::Rc};

use protocol::write_message;

fn main() {
    env_logger::init();
    log::set_max_level(log::LevelFilter::Trace);
    // Connect with echo-network server.
    let mut stream = TcpStream::connect("127.0.0.1:8080")
        .expect("Failed to open socket connection to echoserver.");
    let mut account = otrr::session::Account::new(
        stream.local_addr().unwrap().to_string().into_bytes(),
        Policy::ALLOW_V3
            | Policy::ALLOW_V4
            | Policy::WHITESPACE_START_AKE
            | Policy::ERROR_START_AKE,
        Rc::new(client::Client::new(stream.try_clone().unwrap())),
    )
    .unwrap();
    loop {
        eprintln!("Waiting to receive message…");
        let msg = protocol::read_message(&mut stream).expect("Failed to read message from stream.");
        eprintln!("Processing incoming message…");
        let result = account.session(&msg.0).receive(&msg.1);
        if let Err(err) = result {
            eprintln!("Error processing received message: {:?}", err);
            continue;
        }
        // FIXME convert to string
        let followup = match result.unwrap() {
            UserMessage::None => {
                eprintln!("Message processed without result for user.");
                None
            }
            UserMessage::Error(err) => {
                eprintln!("Error occurred: {:?}", err);
                None
            }
            UserMessage::Plaintext(content) => {
                eprintln!(
                    "Plaintext message: {:?}",
                    String::from_utf8(content.clone()).unwrap()
                );
                Some((0, content))
            }
            UserMessage::WarningUnencrypted(content) => {
                eprintln!("Received unencrypted message: {:?}", content);
                // FIXME add prefix message about "received this unencrypted: ..."
                Some((0, content))
            }
            UserMessage::Confidential(tag, content, tlvs) => {
                eprintln!(
                    "Confidential message on instance {:?} with {} tlvs: {:?}",
                    tag,
                    tlvs.len(),
                    String::from_utf8(content.clone()).unwrap(),
                );
                Some((tag, content))
            }
            UserMessage::ConfidentialSessionStarted(tag) => {
                eprintln!("Confidential session started on {}", tag);
                None
            }
            UserMessage::ConfidentialSessionFinished(tag, content) => {
                eprintln!(
                    "Confidential session finished on {}: {:?}",
                    tag,
                    String::from_utf8(content.clone()).unwrap()
                );
                None
            }
            UserMessage::Reset(tag) => {
                eprintln!("State-reset to plaintext for {}", tag);
                None
            }
            UserMessage::SMPSucceeded(tag) => {
                eprintln!("Successfully completed authentication (SMP) on {}", tag);
                None
            }
            UserMessage::SMPFailed(tag) => {
                eprintln!("Failed authentication (SMP) on {}", tag);
                None
            }
        };
        let (tag, response) = match followup {
            None => continue,
            Some(response) => response,
        };
        match account.session(&msg.0).send(tag, &response) {
            Err(error) => eprintln!("{:?}", error),
            Ok(parts) => {
                for part in parts {
                    write_message(&mut stream, &msg.0, &part)
                        .expect("Failed to transmit message part.");
                }
            }
        }
    }
}

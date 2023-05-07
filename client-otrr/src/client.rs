use std::{cell::RefCell, net::TcpStream, rc::Rc};

use otrr::{clientprofile::ClientProfile, crypto::dsa};

use crate::protocol::write_message;

pub struct Client {
    conn: Rc<RefCell<TcpStream>>,
    keypair: dsa::Keypair,
}

impl otrr::Host for Client {
    fn message_size(&self) -> usize {
        u32::MAX as usize
    }

    fn inject(&self, address: &[u8], message: &[u8]) {
        write_message(&mut (*self.conn.as_ref().borrow_mut()), address, message)
            .expect("Injection of content must succeed.");
    }

    fn keypair(&self) -> &dsa::Keypair {
        &self.keypair
    }

    fn query_smp_secret(&self, question: &[u8]) -> Option<Vec<u8>> {
        todo!()
    }

    fn client_profile(&self) -> &ClientProfile {
        todo!()
    }
}

impl Client {
    pub fn new(conn: Rc<RefCell<TcpStream>>) -> Self {
        Client { conn, keypair: dsa::Keypair::generate() }
    }
}

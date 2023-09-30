use std::{net::TcpStream, sync::RwLock};

use otrr::{crypto::dsa, crypto::ed448};

use crate::protocol::write_message;

pub struct Client {
    pub conn: RwLock<TcpStream>,
    pub keypair: dsa::Keypair,
}

impl otrr::Host for Client {
    fn message_size(&self) -> usize {
        u32::MAX as usize
    }

    fn inject(&self, address: &[u8], message: &[u8]) {
        write_message(&mut (*self.conn.write().unwrap()), address, message)
            .expect("Injection of content must succeed.");
    }

    fn keypair(&self) -> &dsa::Keypair {
        &self.keypair
    }

	fn keypair_identity(&self) -> &ed448::KeyPair {
		todo!()
	}

	fn keypair_forging(&self) -> &ed448::KeyPair {
		todo!()
	}

    fn query_smp_secret(&self, _question: &[u8]) -> Option<Vec<u8>> {
        todo!()
    }

    fn client_profile(&self) -> Vec<u8> {
        todo!()
    }
}

impl Client {
    pub fn new(conn: TcpStream) -> Self {
        Self {
            conn: RwLock::new(conn),
            keypair: dsa::Keypair::generate(),
        }
    }
}

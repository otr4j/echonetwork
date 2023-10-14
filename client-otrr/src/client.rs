use std::{net::TcpStream, sync::RwLock, cell::RefCell};

use otrr::{crypto::dsa, crypto::ed448};

use crate::protocol::write_message;

pub struct Client {
    pub conn: RwLock<TcpStream>,
    pub keypair: dsa::Keypair,
	pub identity: ed448::EdDSAKeyPair,
	pub forging: ed448::EdDSAKeyPair,
	pub payload: RefCell<Vec<u8>>,
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

	fn keypair_identity(&self) -> &ed448::EdDSAKeyPair {
		&self.identity
	}

	fn keypair_forging(&self) -> &ed448::EdDSAKeyPair {
		&self.forging
	}

    fn query_smp_secret(&self, _question: &[u8]) -> Option<Vec<u8>> {
        Some(Self::DEFAULT_SMP_SECRET.to_vec())
    }

    fn client_profile(&self) -> Vec<u8> {
		self.payload.borrow().clone()
    }

	fn update_client_profile(&self, encoded_payload: Vec<u8>) {
		self.payload.replace(encoded_payload);
	}
}

impl Client {
	const DEFAULT_PROTOCOL_NAME: &[u8; 4] = b"echo";
	const DEFAULT_SMP_SECRET: &[u8; 9] = b"Password!";

    pub fn new(conn: TcpStream) -> Self {
        Self {
            conn: RwLock::new(conn),
            keypair: dsa::Keypair::generate(),
			identity: ed448::EdDSAKeyPair::generate(),
			forging: ed448::EdDSAKeyPair::generate(),
			payload: RefCell::new(Vec::new()),
        }
    }
}

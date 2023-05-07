use std::io::{Read, Error, Write};

/// write_message writes a message to the provided buffer.
pub fn write_message(dest: &mut dyn Write, address: &[u8], content: &[u8]) -> Result<usize, Error> {
    let mut buffer = Vec::new();
    write_value(&mut buffer, address);
    write_value(&mut buffer, content);
    dest.write(&buffer)
}

/// read_message reads a message from the provided buffer.
pub fn read_message(src: &mut dyn std::io::Read) -> Result<(Vec<u8>, Vec<u8>), Error> {
    let address = read_value(src)?;
    let content = read_value(src)?;
    Ok((address,content))
}

fn write_value(buffer: &mut Vec<u8>, value: &[u8]) {
    buffer.extend_from_slice(&(value.len() as u32).to_be_bytes());
    buffer.extend_from_slice(value);
}

fn read_value(buffer: &mut dyn Read) -> Result<Vec<u8>, Error> {
    let mut lenbytes = [0u8;4];
    buffer.read_exact(&mut lenbytes)?;
    let len = u32::from_be_bytes([lenbytes[0], lenbytes[1], lenbytes[2], lenbytes[3]]) as usize;
    let mut value = vec![0;len];
    buffer.read_exact(&mut value)?;
    Ok(value)
}

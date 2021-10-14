use crate::room_common::*;

use std::{
    fs::File,
    io::{Read, Write},
    net::{TcpStream, ToSocketAddrs},
    path::PathBuf,
    sync::{
        mpsc::{self, Receiver},
        Mutex,
    },
    thread,
};

#[derive(Debug)]
pub enum ClientError {
    IoError(std::io::Error),
    LoginError(LoginError),
    SendError(SendError),
    UnknownError,
}

#[derive(Debug)]
pub enum LoginError {
    UsernameAlreadyExists,
    UnknownError,
}

#[derive(Debug)]
pub enum SendError {
    NoSuchUsername,
    MessageIsTooLong,
    UnknownError,
}

impl From<std::io::Error> for ClientError {
    fn from(e: std::io::Error) -> Self {
        Self::IoError(e)
    }
}

pub struct Client {
    username: Username,
    send_stream: Mutex<TcpStream>,
    recv_msg: Mutex<Receiver<(Descriptor, ServerHeader, Vec<u8>)>>,
    recv_prot: Mutex<Receiver<Descriptor>>,
    dir: PathBuf,
}

const MAX_BUF_SIZE: usize = 6 * 1024 * 1024;

impl Client {
    pub fn new<A: ToSocketAddrs>(
        username: Username,
        addr: A,
        dir: PathBuf,
    ) -> Result<Self, ClientError> {
        let send_stream = TcpStream::connect(addr)?;
        dbg!(&send_stream);
        let mut recv_stream = send_stream.try_clone()?;
        send_stream.set_nonblocking(false)?;

        let (tx_msg, rx_msg) = mpsc::channel();
        let (tx_prot, rx_prot) = mpsc::channel();
        let client = Self {
            username: username.clone(),
            send_stream: Mutex::new(send_stream),
            recv_msg: Mutex::new(rx_msg),
            recv_prot: Mutex::new(rx_prot),
            dir,
        };

        let dir = client.dir.clone();
        thread::spawn(move || {
            let mut buf = Vec::<u8>::with_capacity(MAX_BUF_SIZE);
            let mut desc_buf = [0u8; std::mem::size_of::<Descriptor>()];
            loop {
                recv_stream.read_exact(&mut desc_buf).unwrap();
                let desc = Descriptor::from_bytes(&desc_buf);
                // assert_eq!(desc.magic_num, DESC_MAGIC_NUM);

                unsafe {
                    buf.set_len(desc.header_size as usize);
                }
                recv_stream.read_exact(&mut buf).unwrap();
                let header = if desc.header_size != 0 {
                    Some(serde_json::from_slice::<ServerHeader>(&buf).unwrap())
                } else {
                    None
                };

                unsafe {
                    buf.set_len(desc.content_size as usize);
                }
                recv_stream.read_exact(&mut buf).unwrap();

                if let Some(ref header) = header {
                    if let Some(ref fname) = header.filename {
                        let mut f = File::create(dir.join(fname)).unwrap();
                        f.write_all(&buf).unwrap();
                    }
                }

                match desc.msg_type {
                    MessageType::Utf8
                    | MessageType::File
                    | MessageType::Login
                    | MessageType::Logout => {
                        tx_msg.send((desc, header.unwrap(), buf.clone())).unwrap()
                    }
                    MessageType::Ack
                    | MessageType::UsernameAlreadyExists
                    | MessageType::IllFormedUsername => tx_prot.send(desc).unwrap(),
                    _ => todo!(),
                }
            }
        });

        Descriptor::from(MessageType::Login)
            .with_content_size(client.username.as_bytes().len() as u64)
            .send(&mut client.send_stream.lock().unwrap())?;
        client
            .send_stream
            .lock()
            .unwrap()
            .write_all(client.username.as_bytes())?;

        let desc1 = client.recv_prot.lock().unwrap().recv().unwrap();
        let desc2 = client.recv_prot.lock().unwrap().recv().unwrap();
        dbg!(&desc2);
        if desc1.msg_type != MessageType::Ack || desc2.msg_type != MessageType::Ack {
            return Err(ClientError::LoginError(LoginError::UnknownError));
        }

        Ok(client)
    }

    pub fn receive(&self) -> Result<(Descriptor, ServerHeader, Vec<u8>), ClientError> {
        Ok(self.recv_msg.lock().unwrap().recv().unwrap())
    }

    pub fn send_text(&self, to: Option<String>, message: String) -> Result<(), ClientError> {
        let mut stream = self.send_stream.lock().unwrap();
        let recv = self.recv_prot.lock().unwrap();
        let header = if to.is_some() {
            serde_json::to_vec(&ClientHeader { to, filename: None }).unwrap()
        } else {
            Vec::new()
        };

        Descriptor::from(MessageType::Utf8)
            .with_content_size(message.as_bytes().len() as u64)
            .with_header_size(header.len() as u16)
            .send(&mut stream)?;
        let ack = recv.recv().unwrap();
        if ack.msg_type != MessageType::Ack {
            return Err(ClientError::SendError(SendError::UnknownError));
        }

        if !header.is_empty() {
            stream.write_all(&header)?;
            let ack = recv.recv().unwrap();
            if ack.msg_type != MessageType::Ack {
                return Err(ClientError::SendError(SendError::UnknownError));
            }
        }

        stream.write_all(message.as_bytes())?;

        Ok(())
    }

    pub fn send_file(&self, to: Option<String>, file: PathBuf) -> Result<(), ClientError> {
        let mut stream = self.send_stream.lock().unwrap();
        let recv = self.recv_prot.lock().unwrap();
        let header = serde_json::to_vec(&ClientHeader {
            to,
            filename: Some(file.file_name().unwrap().to_str().unwrap().to_owned()),
        })
        .unwrap();

        let mut msg = Vec::new();
        File::open(file)?.read_to_end(&mut msg)?;
        Descriptor::from(MessageType::File)
            .with_content_size(msg.len() as u64)
            .with_header_size(header.len() as u16)
            .send(&mut stream)?;

        let ack = recv.recv().unwrap();
        if ack.msg_type != MessageType::Ack {
            return Err(ClientError::SendError(SendError::UnknownError));
        }

        stream.write_all(&header)?;
        let ack = recv.recv().unwrap();
        if ack.msg_type != MessageType::Ack {
            return Err(ClientError::SendError(SendError::UnknownError));
        }

        stream.write_all(&msg)?;

        Ok(())
    }
}

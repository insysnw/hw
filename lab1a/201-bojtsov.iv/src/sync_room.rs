use crate::room_common::*;

use chrono::Utc;
use object_pool::{Pool, Reusable};
use regex::Regex;
use std::collections::HashMap;
use std::fmt::Debug;
use std::fs::File;
use std::fs::OpenOptions;
use std::io::{BufReader, Read, Write};
use std::net::{TcpListener, TcpStream, ToSocketAddrs};
use std::ops::{Deref, DerefMut};
use std::path::PathBuf;
use std::sync::mpsc::{self, Receiver, Sender};
use std::sync::{Arc, Mutex};
use std::thread;

const BUFFER_SIZE: usize = 8 * 1024 * 1024;

#[derive(Debug)]
struct TmpFile {
    path: PathBuf,
}

impl TmpFile {
    fn get_file(&self) -> Result<File, std::io::Error> {
        OpenOptions::new().read(true).write(true).open(&self.path)
    }

    fn new() -> Result<Self, std::io::Error> {
        let regex = Regex::new(r"sync_room_file_([0-9]+)").unwrap();
        let tmp_dir = std::env::temp_dir();

        let mut last_i = 0;
        for entry in std::fs::read_dir(&tmp_dir)? {
            let entry = entry?;
            if entry.path().is_file() {
                if let Some(n) = regex
                    .captures(entry.file_name().to_string_lossy().as_ref())
                    .map(|c| c[1].parse::<u128>().unwrap())
                {
                    last_i = std::cmp::max(n, last_i);
                }
            }
        }

        let path = tmp_dir.join(format!("sync_room_file_{}", last_i.wrapping_add(1)));
        File::create(&path)?;

        Ok(Self { path })
    }
}

impl Drop for TmpFile {
    fn drop(&mut self) {
        std::fs::remove_file(&self.path).unwrap()
    }
}

enum Content {
    File(Arc<TmpFile>),
    Bytes(Arc<Reusable<'static, Vec<u8>>>),
}

impl Debug for Content {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            &Content::File(ref tf) => write!(f, "Content::File({:?})", tf),
            &Content::Bytes(ref c) => write!(f, "Content::Bytes({:?})", c.as_slice()),
        }
        // write!(f, "Hi: {}", self.id)
    }
}

impl Content {
    fn send(self, mut stream: &TcpStream) -> Result<(), std::io::Error> {
        match self {
            Self::File(f) => {
                let mut buf = BUF_POOL.pull(|| Vec::with_capacity(BUFFER_SIZE));
                let mut reader = BufReader::new(f.get_file()?);
                loop {
                    unsafe { buf.set_len(BUFFER_SIZE) };
                    let n = reader.read(&mut buf)?;
                    if n == 0 {
                        break;
                    }
                    stream.write_all(&buf[0..n])?;
                }
                Ok(())
            }
            Self::Bytes(buf) => stream.write_all(&buf),
        }
    }
}

#[derive(Debug)]
enum DescriptorReciever {
    Stream(TcpStream),
    User(Arc<Username>),
}

enum InternalMessage {
    PrivateMessage {
        from: Arc<Username>,
        to: Username,
        desc: Descriptor,
        header: Arc<Reusable<'static, Vec<u8>>>,
        content: Content,
    },
    Broadcast {
        desc: Descriptor,
        header: Arc<Reusable<'static, Vec<u8>>>,
        content: Option<Content>,
    },
    Descriptor {
        reciever: DescriptorReciever,
        desc: Descriptor,
    },
    Logout {
        desc: Descriptor,
        header: Arc<Reusable<'static, Vec<u8>>>,
        username: Arc<Username>,
    },
}

impl Debug for InternalMessage {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            InternalMessage::PrivateMessage {
                from,
                to,
                desc,
                header,
                content,
            } => write!(f, "InternalMessage::PrivateMessage {{\nfrom: {:?}\nto: {:?}\ndesc: {:?}\nheader: {:?}\ncontent: {:?}\n}}", from, to, desc, header.as_slice(), content),
            InternalMessage::Broadcast {
                desc,
                header,
                content,
            } => write!(f, "InternalMessage::Broadcast {{\ndesc: {:?}\nheader: {:?}\ncontent: {:?}\n}}", desc, header.as_slice(), content),
            InternalMessage::Descriptor {
                reciever,
                desc,
            } => write!(f, "InternalMessage::Descriptor {{\nreciever: {:?}\ndesc: {:?}\n}}", reciever, desc),
            InternalMessage::Logout {
                desc,
                header,
                username,
            } => write!(f, "InternalMessage::Logout {{\ndesc: {:?}\nheader: {:?}\nusername: {:?}\n}}", desc, header.as_slice(), username),
        }
    }
}

lazy_static::lazy_static! {
    static ref BUF_POOL: Pool<Vec<u8>> = Pool::new(32, || Vec::with_capacity(BUFFER_SIZE));
}

pub struct SyncRoom {
    connected_users: Mutex<HashMap<Arc<Username>, Sender<InternalMessage>>>,
    listener: TcpListener,
    incoming: Receiver<InternalMessage>,
    sender: Sender<InternalMessage>,
}

// SAFETY: there are always only one thread that is accessing incoming, sender or listener
unsafe impl Sync for SyncRoom {}

impl SyncRoom {
    pub fn new<A: ToSocketAddrs>(addr: A) -> std::io::Result<SyncRoom> {
        let (tx, rx) = mpsc::channel();
        let listener = TcpListener::bind(addr)?;
        let connected_users = Mutex::new(HashMap::new());

        Ok(SyncRoom {
            connected_users,
            listener,
            incoming: rx,
            sender: tx,
        })
    }

    pub fn run(self) -> std::io::Result<()> {
        let server = Arc::new(self);
        let server2 = Arc::clone(&server);

        thread::spawn(move || server2.handle_msg_queue());

        for stream in server.listener.incoming() {
            let stream = stream.unwrap();
            let server = Arc::clone(&server);
            let sender = server.sender.clone();
            thread::spawn(move || Connection::handle(server, stream, sender));
        }

        Ok(())
    }

    fn handle_msg_queue(&self) {
        while let Ok(msg) = self.incoming.recv() {
            match msg {
                InternalMessage::PrivateMessage {
                    from: _,
                    ref to,
                    header: _,
                    desc: _,
                    content: _,
                } => {
                    let _ = self
                        .connected_users
                        .lock()
                        .unwrap()
                        .get(to)
                        .map(|s| s.send(msg));
                }
                InternalMessage::Broadcast {
                    header,
                    desc,
                    content,
                } => {
                    self.connected_users.lock().unwrap().retain(|_, sender| {
                        let content = content.as_ref().map(|c| match c {
                            Content::File(f) => Content::File(Arc::clone(f)),
                            Content::Bytes(ptr) => Content::Bytes(Arc::clone(ptr)),
                        });
                        let msg = InternalMessage::Broadcast {
                            desc,
                            header: Arc::clone(&header),
                            content,
                        };
                        sender.send(msg).is_ok()
                    });
                }
                InternalMessage::Descriptor { reciever, desc } => match reciever {
                    DescriptorReciever::Stream(mut stream) => {
                        thread::spawn(move || desc.send(&mut stream));
                    }
                    DescriptorReciever::User(ref username) => {
                        let _ = self
                            .connected_users
                            .lock()
                            .unwrap()
                            .get(username)
                            .map(|sender| {
                                sender.send(InternalMessage::Descriptor { reciever, desc })
                            });
                    }
                },
                InternalMessage::Logout {
                    desc,
                    header,
                    username,
                } => {
                    let mut map = self.connected_users.lock().unwrap();
                    map.remove(&username);
                    map.retain(|_, sender| {
                        sender
                            .send(InternalMessage::Broadcast {
                                desc,
                                header: Arc::clone(&header),
                                content: None,
                            })
                            .is_ok()
                    });
                }
            };
        }
    }
}

enum ConnectionError {
    IoError(std::io::Error),
    ParsingError,
    SendError,
    BadDescriptor,
    BadUsername,
}

impl From<std::io::Error> for ConnectionError {
    fn from(err: std::io::Error) -> Self {
        Self::IoError(err)
    }
}

impl From<std::sync::mpsc::SendError<InternalMessage>> for ConnectionError {
    fn from(_: std::sync::mpsc::SendError<InternalMessage>) -> Self {
        ConnectionError::SendError
    }
}

struct Connection {
    server: Arc<SyncRoom>,
    stream: TcpStream,
    username: Option<Arc<Username>>,
    sender: Sender<InternalMessage>,
    desc_buf: [u8; std::mem::size_of::<Descriptor>()],
}

impl Connection {
    fn handle(server: Arc<SyncRoom>, stream: TcpStream, sender: Sender<InternalMessage>) {
        let mut connection = Connection {
            server,
            stream,
            username: None,
            sender,
            desc_buf: [0; std::mem::size_of::<Descriptor>()],
        };

        let mut stream = connection.stream.try_clone().unwrap();
        let sender = connection.sender.clone();
        let (tx, rx): (Sender<InternalMessage>, Receiver<InternalMessage>) = mpsc::channel();
        thread::spawn(move || {
            while let Ok(msg) = rx.recv() {
                match msg {
                    InternalMessage::Broadcast {
                        desc,
                        header,
                        content,
                    } => {
                        let _ = desc.send(&mut stream);
                        let _ = stream.write_all(&header);
                        let _ = content.map(|c| c.send(&mut stream));
                    }
                    InternalMessage::PrivateMessage {
                        from,
                        to: _,
                        desc,
                        header,
                        content,
                    } => {
                        let _ = desc.send(&mut stream);
                        let _ = stream.write_all(&header);
                        let _ = content.send(&mut stream);
                        let _ = sender.send(InternalMessage::Descriptor {
                            reciever: DescriptorReciever::User(from),
                            desc: Descriptor::from(MessageType::Ack),
                        });
                    }
                    InternalMessage::Descriptor { desc, .. } => {
                        let _ = desc.send(&mut stream);
                    }
                    _ => unreachable!(),
                }
            }
        });

        while let Err(e) = connection.try_login(tx.clone()) {
            match e {
                ConnectionError::IoError(_) => return,
                _ => {}
            }
        }

        loop {
            let message = connection.get_desc().map(|desc| {
                let msg = connection.get_msg(desc);
                (desc, msg)
            });
            match message {
                Ok((desc, Ok((header, content)))) => {
                    let server_header = ServerHeader {
                        from: connection.username.as_ref().unwrap().deref().clone(),
                        time: Utc::now(),
                        filename: header.filename,
                        private: header.to.is_some(),
                    };
                    let mut buf = BUF_POOL.pull(|| Vec::with_capacity(BUFFER_SIZE));
                    buf.clear();
                    serde_json::to_writer(buf.deref_mut(), &server_header).unwrap();

                    let server_header = Arc::new(buf);
                    let desc = desc.with_header_size(server_header.len() as u16);

                    if let Some(to) = header.to {
                        connection
                            .sender
                            .send(InternalMessage::PrivateMessage {
                                from: Arc::clone(connection.username.as_ref().unwrap()),
                                to: Username::from_string(to).unwrap(),
                                desc,
                                content,
                                header: server_header,
                            })
                            .unwrap();
                    } else {
                        connection
                            .sender
                            .send(InternalMessage::Broadcast {
                                desc,
                                header: server_header,
                                content: Some(content),
                            })
                            .unwrap();
                    }
                }
                Ok((_, Err(ConnectionError::IoError(_)))) | Err(ConnectionError::IoError(_)) => {
                    return
                }
                _ => {}
            }
        }
    }

    fn get_descriptor_reciever(&self) -> Result<DescriptorReciever, ConnectionError> {
        Ok(if let Some(username) = &self.username {
            DescriptorReciever::User(Arc::clone(username))
        } else {
            DescriptorReciever::Stream(self.stream.try_clone()?)
        })
    }

    fn get_msg(&mut self, desc: Descriptor) -> Result<(ClientHeader, Content), ConnectionError> {
        let mut header = {
            let mut buf = [0u8; 256];
            self.stream
                .read_exact(&mut buf[0..desc.header_size as usize])?;
            match serde_json::from_slice::<ClientHeader>(&buf[0..desc.header_size as usize]) {
                Ok(header) => header,
                Err(_) if desc.header_size == 0 => ClientHeader::default(),
                Err(_) => {
                    self.sender.send(InternalMessage::Descriptor {
                        reciever: self.get_descriptor_reciever()?,
                        desc: Descriptor::from(MessageType::BadHeader),
                    })?;
                    return Err(ConnectionError::ParsingError);
                }
            }
        };

        if let Some(to) = header.to {
            let uname = match Username::from_string(to) {
                Ok(uname) => uname,
                Err(_) => {
                    self.sender.send(InternalMessage::Descriptor {
                        reciever: self.get_descriptor_reciever()?,
                        desc: Descriptor::from(MessageType::IllFormedUsername),
                    })?;
                    return Err(ConnectionError::BadUsername);
                }
            };
            header.to = Some(uname.into());
        }

        if desc.header_size != 0 {
            self.sender.send(InternalMessage::Descriptor {
                reciever: self.get_descriptor_reciever()?,
                desc: Descriptor::from(MessageType::Ack),
            })?;
        }

        let mut buf = BUF_POOL.pull(|| Vec::with_capacity(BUFFER_SIZE));
        buf.resize(desc.header_size as usize, 0);

        if desc.content_size > BUFFER_SIZE as u64 {
            let mut remaining = desc.content_size;
            let file = TmpFile::new()?;
            let mut fd = file.get_file()?;
            while remaining != 0 {
                let buf_size = std::cmp::min(BUFFER_SIZE as u64, remaining);
                unsafe {
                    buf.set_len(buf_size as usize);
                }
                self.stream.read_exact(&mut buf)?;
                fd.write_all(&buf)?;

                remaining = remaining.saturating_sub(BUFFER_SIZE as u64);
            }

            Ok((header, Content::File(Arc::new(file))))
        } else {
            unsafe {
                buf.set_len(desc.content_size as usize);
            }
            self.stream.read_exact(&mut buf)?;

            Ok((header, Content::Bytes(Arc::new(buf))))
        }
    }

    fn get_desc(&mut self) -> Result<Descriptor, ConnectionError> {
        self.stream.read_exact(&mut self.desc_buf)?;

        let desc = Descriptor::from_bytes(&self.desc_buf);

        if self.username.is_some() && desc.msg_type == MessageType::Login
            || self.username.is_none() && desc.msg_type != MessageType::Login
            || self.username.is_some() && !desc.msg_type.is_content_type()
            || desc.msg_type == MessageType::Unknown
        {
            self.sender.send(InternalMessage::Descriptor {
                reciever: self.get_descriptor_reciever()?,
                desc: Descriptor::from(MessageType::BadMessageType),
            })?;
            return Err(ConnectionError::BadDescriptor);
        }

        if desc.magic_num != DESC_MAGIC_NUM {
            self.sender.send(InternalMessage::Descriptor {
                reciever: self.get_descriptor_reciever()?,
                desc: Descriptor::from(MessageType::WrongMagicNumber),
            })?;
            return Err(ConnectionError::BadDescriptor);
        }

        if desc.content_size > BUFFER_SIZE as u64 && desc.msg_type != MessageType::File {
            self.sender.send(InternalMessage::Descriptor {
                reciever: self.get_descriptor_reciever()?,
                desc: Descriptor::from(MessageType::MessageIsTooBig),
            })?;
            return Err(ConnectionError::BadDescriptor);
        }

        self.sender.send(InternalMessage::Descriptor {
            reciever: self.get_descriptor_reciever()?,
            desc: Descriptor::from(MessageType::Ack),
        })?;

        Ok(desc)
    }

    fn try_login(&mut self, sender: Sender<InternalMessage>) -> Result<(), ConnectionError> {
        let desc = self.get_desc()?;

        let mut buf = BUF_POOL.pull(|| Vec::with_capacity(BUFFER_SIZE));
        buf.resize(desc.content_size as usize, 0);
        self.stream.read_exact(&mut buf)?;

        let uname = match String::from_utf8(buf.iter().copied().collect()) {
            Ok(uname) => uname,
            Err(_) => {
                self.sender.send(InternalMessage::Descriptor {
                    reciever: self.get_descriptor_reciever()?,
                    desc: Descriptor::from(MessageType::IllFormedUsername),
                })?;
                return Err(ConnectionError::BadUsername);
            }
        };

        let uname = match Username::from_string(uname) {
            Ok(uname) => uname,
            Err(_) => {
                self.sender.send(InternalMessage::Descriptor {
                    reciever: self.get_descriptor_reciever()?,
                    desc: Descriptor::from(MessageType::IllFormedUsername),
                })?;
                return Err(ConnectionError::BadUsername);
            }
        };

        let mut map = self.server.connected_users.lock().unwrap();
        if map.contains_key(&uname) {
            drop(map);
            self.sender.send(InternalMessage::Descriptor {
                reciever: self.get_descriptor_reciever()?,
                desc: Descriptor::from(MessageType::UsernameAlreadyExists)
                    .with_header_size(desc.header_size),
            })?;
            return Err(ConnectionError::BadUsername);
        }

        self.sender.send(InternalMessage::Descriptor {
            reciever: self.get_descriptor_reciever()?,
            desc: Descriptor::from(MessageType::Ack).with_header_size(desc.header_size),
        })?;

        self.username = Some(Arc::new(uname.clone()));
        map.insert(Arc::clone(self.username.as_ref().unwrap()), sender);
        drop(map);

        let header = ServerHeader {
            from: uname,
            time: Utc::now(),
            filename: None,
            private: false,
        };
        let mut buf = BUF_POOL.pull(|| Vec::with_capacity(BUFFER_SIZE));
        buf.clear();
        serde_json::to_writer(buf.deref_mut(), &header).unwrap();
        let header = Arc::new(buf);

        self.sender.send(InternalMessage::Broadcast {
            desc: Descriptor::from(MessageType::Login).with_header_size(header.len() as u16),
            header,
            content: None,
        })?;

        Ok(())
    }
}

impl Drop for Connection {
    fn drop(&mut self) {
        if let Some(username) = &self.username {
            let header = ServerHeader {
                from: username.as_ref().clone(),
                time: Utc::now(),
                filename: None,
                private: false,
            };

            let mut buf = BUF_POOL.pull(|| Vec::with_capacity(BUFFER_SIZE));
            buf.clear();
            serde_json::to_writer(buf.deref_mut(), &header).unwrap();
            let header = Arc::new(buf);

            let _ = self.sender.send(InternalMessage::Logout {
                desc: Descriptor::from(MessageType::Logout).with_header_size(header.len() as u16),
                header,
                username: Arc::clone(&username),
            });
        }
    }
}

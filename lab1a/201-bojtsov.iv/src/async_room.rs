use crate::room_common::*;

use chrono::Utc;
use object_pool::{Pool, Reusable};
use regex::Regex;
use std::collections::HashMap;
use std::fmt::Debug;
use std::ops::{Deref, DerefMut};
use std::path::PathBuf;
use std::sync::Arc;
use tokio::fs::File;
use tokio::fs::OpenOptions;
use tokio::io::AsyncBufReadExt;
use tokio::io::AsyncWriteExt;
use tokio::io::{AsyncReadExt, BufReader};
use tokio::net::tcp::{OwnedReadHalf, OwnedWriteHalf};
use tokio::net::{TcpListener, TcpStream, ToSocketAddrs};
use tokio::sync::mpsc::{self, Receiver, Sender};
use tokio::sync::Mutex;

const BUFFER_SIZE: usize = 8 * 1024 * 1024;

#[derive(Debug)]
struct TmpFile {
    path: PathBuf,
}

impl TmpFile {
    async fn get_file(&self) -> tokio::io::Result<File> {
        OpenOptions::new()
            .read(true)
            .write(true)
            .open(&self.path)
            .await
    }

    async fn new() -> tokio::io::Result<Self> {
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
        File::create(&path).await?;

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
    }
}

impl Content {
    async fn send<T>(self, stream: &mut T) -> tokio::io::Result<()>
    where
        T: AsyncWriteExt + std::marker::Unpin,
    {
        match self {
            Self::File(f) => {
                let mut buf = BUF_POOL.pull(|| Vec::with_capacity(BUFFER_SIZE));
                let mut reader = BufReader::new(f.get_file().await?);
                loop {
                    unsafe { buf.set_len(BUFFER_SIZE) };
                    let n = reader.read_until(0, &mut buf).await?;
                    if n == 0 {
                        break;
                    }
                    stream.write_all(&buf[0..n]).await?;
                }
                Ok(())
            }
            Self::Bytes(buf) => stream.write_all(&buf).await,
        }
    }
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
        reciever: Arc<Username>,
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

pub struct AsyncRoom {
    connected_users: Mutex<HashMap<Arc<Username>, Sender<InternalMessage>>>,
    listener: TcpListener,
    incoming: Mutex<Receiver<InternalMessage>>,
    sender: Sender<InternalMessage>,
}

// SAFETY: there are always only one thread that is accessing incoming, sender or listener
unsafe impl Sync for AsyncRoom {}

impl AsyncRoom {
    pub async fn new<A: ToSocketAddrs>(addr: A) -> tokio::io::Result<AsyncRoom> {
        let (tx, rx) = mpsc::channel(16);
        let listener = TcpListener::bind(addr).await?;
        let connected_users = Mutex::new(HashMap::new());

        Ok(AsyncRoom {
            connected_users,
            listener,
            incoming: Mutex::new(rx),
            sender: tx,
        })
    }

    pub async fn run(self) -> tokio::io::Result<()> {
        let server = Arc::new(self);
        let server2 = Arc::clone(&server);

        tokio::spawn(async {
            let server = server2;
            let _ = server.handle_msg_queue().await;
        });

        while let Ok((stream, _)) = server.listener.accept().await {
            let server = Arc::clone(&server);
            let sender = server.sender.clone();
            tokio::spawn(async { Connection::handle(server, stream, sender).await });
        }

        Ok(())
    }

    async fn handle_msg_queue(&self) -> tokio::io::Result<()> {
        while let Some(msg) = self.incoming.lock().await.recv().await {
            match msg {
                InternalMessage::PrivateMessage {
                    from: _,
                    ref to,
                    header: _,
                    desc: _,
                    content: _,
                } => {
                    if let Some(s) = self.connected_users.lock().await.get(to) {
                        let _ = s.send(msg).await;
                    }
                }
                InternalMessage::Broadcast {
                    header,
                    desc,
                    content,
                } => {
                    for (_, sender) in self.connected_users.lock().await.iter() {
                        let content = content.as_ref().map(|c| match c {
                            Content::File(f) => Content::File(Arc::clone(f)),
                            Content::Bytes(ptr) => Content::Bytes(Arc::clone(ptr)),
                        });
                        let msg = InternalMessage::Broadcast {
                            desc,
                            header: Arc::clone(&header),
                            content,
                        };
                        let _ = sender.send(msg).await;
                    }
                }
                InternalMessage::Descriptor { reciever, desc } => {
                    if let Some(sender) = self.connected_users.lock().await.get(&reciever) {
                        let _ = sender
                            .send(InternalMessage::Descriptor { reciever, desc })
                            .await;
                    }
                }
                InternalMessage::Logout {
                    desc,
                    header,
                    username,
                } => {
                    let mut map = self.connected_users.lock().await;
                    map.remove(&username);
                    for (_, sender) in map.iter() {
                        let _ = sender
                            .send(InternalMessage::Broadcast {
                                desc,
                                header: Arc::clone(&header),
                                content: None,
                            })
                            .await;
                    }
                }
            };
        }
        Ok(())
    }
}

#[derive(Debug)]
enum ConnectionError {
    IoError(std::io::Error),
    ParsingError,
    SendError,
    BadDescriptor,
    BadUsername,
}

impl From<tokio::io::Error> for ConnectionError {
    fn from(err: tokio::io::Error) -> Self {
        Self::IoError(err)
    }
}

impl From<std::sync::mpsc::SendError<InternalMessage>> for ConnectionError {
    fn from(_: std::sync::mpsc::SendError<InternalMessage>) -> Self {
        ConnectionError::SendError
    }
}

struct Connection {
    server: Arc<AsyncRoom>,
    // stream: TcpStream,
    stream_read: OwnedReadHalf,
    username: Option<Arc<Username>>,
    sender: Sender<InternalMessage>,
    desc_buf: [u8; std::mem::size_of::<Descriptor>()],
}

impl Connection {
    async fn handle(server: Arc<AsyncRoom>, stream: TcpStream, sender: Sender<InternalMessage>) {
        let (stream_read, mut stream_write) = stream.into_split();

        let mut connection = Connection {
            server,
            // stream,
            stream_read,
            username: None,
            sender,
            desc_buf: [0; std::mem::size_of::<Descriptor>()],
        };

        let sender = connection.sender.clone();
        let (tx, rx): (Sender<InternalMessage>, Receiver<InternalMessage>) = mpsc::channel(16);

        let msg = loop {
            let h = connection.try_login(tx.clone(), &mut stream_write).await;
            match h {
                Ok(msg) => break msg,
                Err(ConnectionError::IoError(_)) => return,
                _ => continue,
            }
        };

        Connection::spawn_writer(rx, sender, stream_write);

        connection
            .sender
            .send(InternalMessage::Descriptor {
                desc: Descriptor::from(MessageType::Ack),
                reciever: Arc::clone(connection.username.as_ref().unwrap()),
            })
            .await
            .unwrap();

        connection.sender.send(msg).await.unwrap();

        loop {
            let desc = connection.get_desc().await;
            match desc {
                Err(ConnectionError::IoError(_)) => return,
                Err(_) => continue,
                _ => {}
            }
            // if desc.is_err() {
            //     continue;
            // }
            let desc = desc.ok().unwrap();
            let message = (desc, connection.get_msg(desc).await);
            match message {
                (desc, Ok((header, content))) => {
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
                        let _ = connection
                            .sender
                            .send(InternalMessage::PrivateMessage {
                                from: Arc::clone(connection.username.as_ref().unwrap()),
                                to: Username::from_string(to).unwrap(),
                                desc,
                                content,
                                header: server_header,
                            })
                            .await;
                    } else {
                        let _ = connection
                            .sender
                            .send(InternalMessage::Broadcast {
                                desc,
                                header: server_header,
                                content: Some(content),
                            })
                            .await;
                    }
                }
                (_, Err(ConnectionError::IoError(_))) => return,
                _ => {}
            }
        }
    }

    fn spawn_writer(
        mut rx: Receiver<InternalMessage>,
        sender: Sender<InternalMessage>,
        mut stream_write: OwnedWriteHalf,
    ) {
        tokio::spawn(async move {
            while let Some(msg) = rx.recv().await {
                match msg {
                    InternalMessage::Broadcast {
                        desc,
                        header,
                        content,
                    } => {
                        let _ = stream_write.write_all(desc.as_bytes()).await;
                        // let _ = desc.send_async(&mut stream_write);
                        let _ = stream_write.write_all(&header).await;
                        if let Some(content) = content {
                            let _ = content.send(&mut stream_write).await;
                        }
                    }
                    InternalMessage::PrivateMessage {
                        from,
                        to: _,
                        desc,
                        header,
                        content,
                    } => {
                        let _ = stream_write.write_all(desc.as_bytes()).await;
                        // let _ = desc.send(&mut stream_write);
                        let _ = stream_write.write_all(&header).await;
                        let _ = content.send(&mut stream_write).await;
                        let _ = sender
                            .send(InternalMessage::Descriptor {
                                reciever: from,
                                desc: Descriptor::from(MessageType::Ack),
                            })
                            .await;
                    }
                    InternalMessage::Descriptor { desc, .. } => {
                        let _ = stream_write.write_all(desc.as_bytes()).await;
                        // let _ = desc.send(&mut stream_write);
                    }
                    _ => unreachable!(),
                }
            }
        });
    }

    async fn get_msg(
        &mut self,
        desc: Descriptor,
    ) -> Result<(ClientHeader, Content), ConnectionError> {
        let mut header = {
            let mut buf = [0u8; 256];
            self.stream_read
                .read_exact(&mut buf[0..desc.header_size as usize])
                .await?;
            match serde_json::from_slice::<ClientHeader>(&buf[0..desc.header_size as usize]) {
                Ok(header) => header,
                Err(_) if desc.header_size == 0 => ClientHeader::default(),
                Err(_) => {
                    let _ = self
                        .sender
                        .send(InternalMessage::Descriptor {
                            reciever: Arc::clone(self.username.as_ref().unwrap()),
                            desc: Descriptor::from(MessageType::BadHeader),
                        })
                        .await;
                    return Err(ConnectionError::ParsingError);
                }
            }
        };

        if let Some(to) = header.to {
            let uname = match Username::from_string(to) {
                Ok(uname) => uname,
                Err(_) => {
                    let _ = self
                        .sender
                        .send(InternalMessage::Descriptor {
                            reciever: Arc::clone(self.username.as_ref().unwrap()),
                            desc: Descriptor::from(MessageType::IllFormedUsername),
                        })
                        .await;
                    return Err(ConnectionError::BadUsername);
                }
            };
            header.to = Some(uname.into());
        }

        if desc.header_size != 0 {
            let _ = self
                .sender
                .send(InternalMessage::Descriptor {
                    reciever: Arc::clone(self.username.as_ref().unwrap()),
                    desc: Descriptor::from(MessageType::Ack),
                })
                .await;
        }

        let mut buf = BUF_POOL.pull(|| Vec::with_capacity(BUFFER_SIZE));
        buf.resize(desc.header_size as usize, 0);

        if desc.content_size > BUFFER_SIZE as u64 {
            let mut remaining = desc.content_size;
            let file = TmpFile::new().await?;
            let mut fd = file.get_file().await?;
            while remaining != 0 {
                let buf_size = std::cmp::min(BUFFER_SIZE as u64, remaining);
                unsafe {
                    buf.set_len(buf_size as usize);
                }
                self.stream_read.read_exact(&mut buf).await?;
                fd.write_all(&buf).await?;

                remaining = remaining.saturating_sub(BUFFER_SIZE as u64);
            }

            Ok((header, Content::File(Arc::new(file))))
        } else {
            unsafe {
                buf.set_len(desc.content_size as usize);
            }
            self.stream_read.read_exact(&mut buf).await?;

            Ok((header, Content::Bytes(Arc::new(buf))))
        }
    }

    async fn get_desc(&mut self) -> Result<Descriptor, ConnectionError> {
        self.stream_read.read_exact(&mut self.desc_buf).await?;

        let desc = Descriptor::from_bytes(&self.desc_buf);

        if self.username.is_some() && desc.msg_type == MessageType::Login
            || self.username.is_none() && desc.msg_type != MessageType::Login
            || self.username.is_some() && !desc.msg_type.is_content_type()
            || desc.msg_type == MessageType::Unknown
        {
            let _ = self
                .sender
                .send(InternalMessage::Descriptor {
                    reciever: Arc::clone(self.username.as_ref().unwrap()),
                    desc: Descriptor::from(MessageType::BadMessageType),
                })
                .await;
            return Err(ConnectionError::BadDescriptor);
        }

        if desc.magic_num != DESC_MAGIC_NUM {
            let _ = self
                .sender
                .send(InternalMessage::Descriptor {
                    reciever: Arc::clone(self.username.as_ref().unwrap()),
                    desc: Descriptor::from(MessageType::WrongMagicNumber),
                })
                .await;
            return Err(ConnectionError::BadDescriptor);
        }
        if desc.msg_type == MessageType::Unknown {
            let _ = self
                .sender
                .send(InternalMessage::Descriptor {
                    reciever: Arc::clone(self.username.as_ref().unwrap()),
                    desc: Descriptor::from(MessageType::BadMessageType),
                })
                .await;
            return Err(ConnectionError::BadDescriptor);
        }
        if desc.content_size > BUFFER_SIZE as u64 && desc.msg_type != MessageType::File {
            let _ = self
                .sender
                .send(InternalMessage::Descriptor {
                    reciever: Arc::clone(self.username.as_ref().unwrap()),
                    desc: Descriptor::from(MessageType::MessageIsTooBig),
                })
                .await;
            return Err(ConnectionError::BadDescriptor);
        }

        let _ = self
            .sender
            .send(InternalMessage::Descriptor {
                reciever: Arc::clone(self.username.as_ref().unwrap()),
                desc: Descriptor::from(MessageType::Ack),
            })
            .await;

        Ok(desc)
    }

    async fn try_login(
        &mut self,
        sender: Sender<InternalMessage>,
        writer: &mut OwnedWriteHalf,
    ) -> Result<InternalMessage, ConnectionError> {
        self.stream_read.read_exact(&mut self.desc_buf).await?;

        let desc = Descriptor::from_bytes(&self.desc_buf);
        if desc.magic_num != DESC_MAGIC_NUM {
            writer
                .write_all(Descriptor::from(MessageType::WrongMagicNumber).as_bytes())
                .await?;
            return Err(ConnectionError::BadDescriptor);
        }
        if desc.msg_type == MessageType::Unknown {
            writer
                .write_all(Descriptor::from(MessageType::BadMessageType).as_bytes())
                .await?;
            return Err(ConnectionError::BadDescriptor);
        }
        if desc.content_size > BUFFER_SIZE as u64 && desc.msg_type != MessageType::File {
            writer
                .write_all(Descriptor::from(MessageType::MessageIsTooBig).as_bytes())
                .await?;
            return Err(ConnectionError::BadDescriptor);
        }

        writer
            .write_all(Descriptor::from(MessageType::Ack).as_bytes())
            .await?;

        let mut buf = BUF_POOL.pull(|| Vec::with_capacity(BUFFER_SIZE));
        buf.resize(desc.content_size as usize, 0);
        self.stream_read.read_exact(&mut buf).await?;

        let uname = match String::from_utf8(buf.iter().copied().collect()) {
            Ok(uname) => uname,
            Err(_) => {
                writer
                    .write_all(Descriptor::from(MessageType::IllFormedUsername).as_bytes())
                    .await?;
                return Err(ConnectionError::BadUsername);
            }
        };

        let uname = match Username::from_string(uname) {
            Ok(uname) => uname,
            Err(_) => {
                writer
                    .write_all(Descriptor::from(MessageType::IllFormedUsername).as_bytes())
                    .await?;
                return Err(ConnectionError::BadUsername);
            }
        };

        let mut map = self.server.connected_users.lock().await;
        if map.contains_key(&uname) {
            drop(map);
            writer
                .write_all(Descriptor::from(MessageType::UsernameAlreadyExists).as_bytes())
                .await?;
            return Err(ConnectionError::BadUsername);
        }

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

        Ok(InternalMessage::Broadcast {
            desc: Descriptor::from(MessageType::Login).with_header_size(header.len() as u16),
            header,
            content: None,
        })
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

            let sender = self.sender.clone();
            let username = Arc::clone(&username);
            tokio::spawn(async move {
                let _ = sender
                    .send(InternalMessage::Logout {
                        desc: Descriptor::from(MessageType::Logout)
                            .with_header_size(header.len() as u16),
                        header,
                        username,
                    })
                    .await;
            });
        }
    }
}

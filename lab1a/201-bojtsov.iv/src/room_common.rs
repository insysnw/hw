use chrono::{DateTime, Utc};
use regex::Regex;
use serde::{Deserialize, Serialize};
use std::{convert::TryFrom, io::Write, net::TcpStream, ops::Deref};
use tokio::io::AsyncWriteExt;
// use std::ops::DerefMut;

pub const DESC_MAGIC_NUM: u16 = 0xBEEF;

/// Username can contain only english characters, numbers and underscores and must be encoded with UTF-8 and less or equal to 32 characters
#[repr(transparent)]
#[derive(Deserialize, Serialize, Debug, PartialEq, Eq, Hash, Clone)]
pub struct Username {
    username: String,
}

impl Username {
    pub fn from_string(s: String) -> Result<Username, ()> {
        let u = Username { username: s };
        if u.is_ill_formed() {
            Err(())
        } else {
            Ok(u)
        }
    }

    pub fn is_ill_formed(&self) -> bool {
        let re = Regex::new(r"[^A-Za-z0-9_]").unwrap();
        let s = self.username.as_str();
        s.len() > 32 || re.is_match(s)
    }
}

impl Deref for Username {
    type Target = String;
    fn deref(&self) -> &Self::Target {
        &self.username
    }
}

impl Into<String> for Username {
    fn into(self) -> String {
        self.username
    }
}

// impl DerefMut for Username {
//     fn deref_mut(&mut self) -> &mut Self::Target {
//         &mut self.0
//     }
// }

#[derive(Deserialize, Serialize, Debug, Default)]
pub struct ClientHeader {
    #[serde(skip_serializing_if = "Option::is_none")]
    #[serde(default = "Default::default")]
    #[serde(rename(serialize = "u", deserialize = "u"))]
    pub to: Option<String>,

    #[serde(skip_serializing_if = "Option::is_none")]
    #[serde(default = "Default::default")]
    #[serde(rename(serialize = "f", deserialize = "f"))]
    pub filename: Option<String>,
}

#[derive(Deserialize, Serialize, Debug, Clone)]
pub struct ServerHeader {
    #[serde(flatten)]
    pub from: Username,
    pub time: DateTime<Utc>,

    #[serde(rename(serialize = "p", deserialize = "p"))]
    pub private: bool,

    #[serde(skip_serializing_if = "Option::is_none")]
    #[serde(default = "Default::default")]
    #[serde(rename(serialize = "f", deserialize = "f"))]
    pub filename: Option<String>,
}

#[repr(C, packed)]
#[derive(Clone, Copy, Debug)]
pub struct Descriptor {
    pub content_size: u64,
    pub header_size: u16,
    pub magic_num: u16,
    pub msg_type: MessageType,
}

impl Descriptor {
    // #[cfg(target_endian = "little")]
    pub fn from_bytes(bytes: &[u8]) -> Self {
        let (bytes, _) = bytes.split_at(std::mem::size_of::<Descriptor>());
        Self {
            magic_num: ((bytes[11] as u16) << 8) | (bytes[10] as u16),
            msg_type: MessageType::try_from(bytes[12]).unwrap(),
            header_size: ((bytes[9] as u16) << 8) | (bytes[8] as u16),
            content_size: unsafe { *std::mem::transmute::<_, &u64>(bytes.as_ptr()) },
        }
    }

    // #[cfg(target_endian = "little")]
    pub fn send(self, stream: &mut TcpStream) -> std::io::Result<()> {
        let len = std::mem::size_of::<Self>();
        let slice = unsafe { std::slice::from_raw_parts(&self as *const Self as *const u8, len) };
        stream.write_all(slice)?;
        Ok(())
    }

    // #[cfg(target_endian = "little")]
    pub async fn send_async(self, stream: &mut tokio::net::TcpStream) -> tokio::io::Result<()> {
        let len = std::mem::size_of::<Self>();
        let slice = unsafe { std::slice::from_raw_parts(&self as *const Self as *const u8, len) };
        stream.write_all(slice).await?;
        Ok(())
    }

    pub fn as_bytes(&self) -> &[u8] {
        let len = std::mem::size_of::<Self>();
        unsafe { std::slice::from_raw_parts(self as *const Self as *const u8, len) }
    }

    pub fn with_header_size(mut self, id: u16) -> Self {
        self.header_size = id;
        self
    }

    pub fn with_content_size(mut self, len: u64) -> Self {
        self.content_size = len;
        self
    }

    pub fn with_msg_type(mut self, msg_type: MessageType) -> Self {
        self.msg_type = msg_type;
        self
    }
}

impl Default for Descriptor {
    fn default() -> Self {
        Self {
            magic_num: DESC_MAGIC_NUM,
            msg_type: MessageType::Unknown,
            header_size: 0,
            content_size: 0,
        }
    }
}

impl From<MessageType> for Descriptor {
    fn from(msg_type: MessageType) -> Self {
        Self {
            magic_num: DESC_MAGIC_NUM,
            msg_type,
            header_size: 0,
            content_size: 0,
        }
    }
}

#[repr(u8)]
#[derive(num_enum::TryFromPrimitive, Debug, Clone, Copy, PartialEq, Eq)]
pub enum MessageType {
    // Content type
    Utf8,
    VoiceMessage,
    Image,
    Stricker,
    File,

    // Server Response
    Ack,
    NoSuchUsername,
    BadMessageType,
    IllFormedUsername,
    MessageIsTooBig,
    BadHeader,
    WrongMagicNumber,
    UsernameAlreadyExists,

    // Other
    Login,
    Logout,

    #[num_enum(default)]
    Unknown,
}

impl MessageType {
    pub fn is_content_type(&self) -> bool {
        self == &Self::Utf8
            || self == &Self::VoiceMessage
            || self == &Self::Image
            || self == &Self::Stricker
            || self == &Self::File
    }

    pub fn is_server_response(&self) -> bool {
        self == &Self::Ack
            || self == &Self::NoSuchUsername
            || self == &Self::BadMessageType
            || self == &Self::IllFormedUsername
            || self == &Self::MessageIsTooBig
            || self == &Self::BadHeader
            || self == &Self::WrongMagicNumber
            || self == &Self::UsernameAlreadyExists
    }
}

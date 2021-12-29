# Even More Primitive TCP Chat Protocol

[introduction]

## Data conventions

**UTF-8 character encoding** is implied when it comes to byte representation of any string.

**Big endianness** is implied when it comes to byte representation of any number.

All timestamps are **64-bit** and in **Unix time** (number of seconds since 00:00:00 UTC on 1 January 1970).

## Packets

### General packet structure

Each packet consists of a *header* and a *body*. A packet header is 2 bytes long and constitutes a 1-byte-long field for
the **protocol version** in use and a 1-byte-long field for the **event** denoted by the packet. A packet body is
determined solely by the packet's event.

For the purposes of this document, the protocol version of all packets is considered equal to 1.

The following section describes all packets defined by this protocol. There are 16 available packets in total.

### Individual packets

#### `ConnectionRequest` packet

[TODO]

#### `ConnectionAccepted` packet

[TODO]

#### `ConnectionNotification` packet

[TODO]

#### `ConnectionRejected` packet

[TODO]

#### `DisconnectionNotification` packet

[TODO]

#### `MessageSent` packet

[TODO]

#### `MessageNotification` packet

[TODO]

#### `FileUpload` packet

[TODO]

#### `FileUploadAccepted` packet

[TODO]

#### `FileUploaded` packet

[TODO]

#### `FileNotification` packet

[TODO]

#### `FileUploadRejected` packet

[TODO]

#### `FileDownload` packet

[TODO]

#### `FileDownloadAccepted` packet

[TODO]

#### `FileDownloaded` packet

[TODO]

#### `FileDownloadRejected` packet

[TODO]

## Interactions

[TODO]

# Even More Primitive TCP Chat

[TODO]

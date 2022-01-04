# Even More Primitive TCP Chat Protocol

[introduction]

## Data conventions

**Big endianness** is implied when it comes to any byte representation.

**UTF-8 character encoding** is implied when it comes to byte representation of any string.

All timestamps are **64-bit** and in **Unix time** (number of seconds since 00:00:00 UTC on 1 January 1970).

## Packets

### General packet structure

Each packet consists of a *header* and a *body*. A packet header is 2 bytes long and constitutes a 1-byte-long field for
the **protocol version** in use and a 1-byte-long field for the **event** denoted by the packet. A packet body is
determined solely by the packet's event.

For the purposes of this document, the protocol version field of all packets is considered equal to 1.

The following section describes all packets defined by this protocol. There are 16 available packets in total.

### Individual packets

#### `ConnectionRequest` packet

These packets have the event field equal to 0.

The packet body (in that order):
* `usernameLength` (number, 1 byte)
* `username` (string, `usernameLength` bytes, up to 256 bytes)

These packets are sent from a client to the server.

#### `ConnectionAccepted` packet

These packets have the event field equal to 1.

These packets do not have a packet body.

These packets are sent from the server to clients.

#### `ConnectionNotification` packet

These packets have the event field equal to 2.

The packet body (in that order):
* `timestamp` (timestamp)
* `usernameLength` (number, 1 byte)
* `username` (string, `usernameLength` bytes, up to 256 bytes)

These packets are sent from the server to clients.

#### `ConnectionRejected` packet

These packets have the event field equal to 3.

These packets do not have a packet body.

These packets are sent from the server to clients.

#### `DisconnectionNotification` packet

These packets have the event field equal to 4.

The packet body (in that order):
* `timestamp` (timestamp)
* `usernameLength` (number, 1 byte)
* `username` (string, `usernameLength` bytes, up to 256 bytes)

These packets are sent from the server to clients.

#### `MessageSent` packet

These packets have the event field equal to 5.

The packet body (in that order):
* `messageLength` (number, 2 bytes)
* `message` (string, `messageLength` bytes, up to 64 KiB)

These packets are sent from a client to the server.

#### `MessageNotification` packet

These packets have the event field equal to 6.

The packet body (in that order):
* `timestamp` (timestamp)
* `usernameLength` (number, 1 byte)
* `messageLength` (number, 2 bytes)
* `username` (string, `usernameLength` bytes, up to 256 bytes)
* `message` (string, `messageLength` bytes, up to 64 KiB)

These packets are sent from the server to clients.

#### `FileUpload` packet

These packets have the event field equal to 7.

The packet body (in that order):
* `fileId` (number, 2 bytes)
* `fileSizeInfo` (number, 3 bytes)
* `fileExtensionLength` (number, 1 byte)
* `fileExtension` (string, `fileExtensionLength` bytes, up to 256 bytes)

These packets are sent from a client to the server.

#### `FileUploadAccepted` packet

These packets have the event field equal to 8.

The packet body (in that order):
* `fileId` (number, 2 bytes)
* `socketPort` (number, 2 bytes)

These packets are sent from the server to clients.

#### `FileUploaded` packet

These packets have the event field equal to 9.

The packet body (in that order):
* `file` (binary data, up to 16 MiB)

These packets are sent from a client to the server.

#### `FileNotification` packet

These packets have the event field equal to 10.

The packet body (in that order):
* `timestamp` (timestamp)
* `fileSizeInfo` (number, 3 bytes)
* `usernameLength` (number, 1 byte)
* `filenameLength` (number, 2 bytes)
* `username` (string, `usernameLength` bytes, up to 256 bytes)
* `filename` (string, `filenameLength` bytes, up to 64 KiB)

These packets are sent from the server to clients.

#### `FileUploadRejected` packet

These packets have the event field equal to 11.

The packet body (in that order):
* `fileId` (number, 2 bytes)

These packets are sent from the server to clients.

#### `FileDownload` packet

These packets have the event field equal to 12.

The packet body (in that order):
* `filenameLength` (number, 2 bytes)
* `filename` (string, `filenameLength` bytes, up to 64 KiB)

These packets are sent from a client to the server.

#### `FileDownloadAccepted` packet

These packets have the event field equal to 13.

The packet body (in that order):
* `socketPort` (number, 2 bytes)
* `filenameLength` (number, 2 bytes)
* `filename` (string, `filenameLength` bytes, up to 64 KiB)

These packets are sent from the server to clients.

#### `FileDownloaded` packet

These packets have the event field equal to 14.

The packet body (in that order):
* `fileSize` (number, 3 bytes)
* `file` (binary data, `fileSize` bytes, up to 16 MiB)

These packets are sent from the server to clients.

#### `FileDownloadRejected` packet

These packets have the event field equal to 15.

The packet body (in that order):
* `filenameLength` (number, 2 bytes)
* `filename` (string, `filenameLength` bytes, up to 64 KiB)

These packets are sent from the server to clients.

## Interactions

[TODO]

# Even More Primitive TCP Chat

[TODO]

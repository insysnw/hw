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

The following section describes all packets defined by this protocol. There are 10 available packets in total.

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

#### `FileSent` packet

These packets have the event field equal to 7.

The packet body (in that order):
* `filenameLength` (number, 1 byte)
* `fileSize` (number, 3 bytes)
* `filename` (string, `messageLength` bytes, up to 256 bytes)
* `file` (string, `fileSize` bytes, up to 16 MiB)

These packets are sent from a client to the server.

#### `FileNotificationPending` packet

These packets have the event field equal to 8.

The packet body (in that order):
* `socketPort` (number, 2 bytes)
* `expiryTime` (timestamp)

These packets are sent from the server to clients.

#### `FileNotification` packet

These packets have the event field equal to 9.

The packet body (in that order):
* `timestamp` (timestamp)
* `usernameLength` (number, 1 byte)
* `filenameLength` (number, 1 byte)
* `fileSize` (number, 3 bytes)
* `username` (string, `usernameLength` bytes, up to 256 bytes)
* `filename` (string, `filenameLength` bytes, up to 256 bytes)
* `file` (string, `fileSize` bytes, up to 16 MiB)

These packets are sent from the server to clients.

## Interactions

### Client connection

A client should send a `ConnectionRequest` packet to the server after establishing a TCP connection. The
client can reply with either `ConnectionAccepted` or `ConnectionRejected`. It is up to the server to
come up with the rules of choosing between the two — possible use cases include maintaining uniqueness of
usernames or filtering them. The simplest way of handling this is to always send a `ConnectionAccepted` packet.

If the server has accepted the connection request, it must also send `ConnectionNotification` packets to other
connected clients. No additional actions are required if the server has rejected the connection.

The server is free to ignore or disconnect any client who doesn't follow this procedure. 

### Client disconnection

A client disconnects simply by closing its TCP connection. The server must send `DisconnectionNotification` packets to
other connected clients if the client in question has successfully passed the connection procedure.

### Sending a message

A client sends a message by sending a `MessageSent` packet to the server. The server then sends `MessageNotification`
packets to all connected clients (including the sender) based on the received `MessageSent` packet.

### Sending a file

A client sends a file by sending a `FileSent` packet to the server.

Upon receiving a `FileSent` packet, the server chooses 1) a free port to listen to connections at and 2) the moment in
time at which the port in question will be released. This info is sent to all connected clients (excluding the sender)
via a `FileNotificationPending` notification.

Upon receiving a `FileNotificationPending` packet, a client must establish another connection to the server on a provided
port. However, a client should ignore the packet if its expiry time has already passed.

Upon each connection to a delegated port, the server should send a `FileNotification` packet via a new connection. It is
expected that a client will close the connection itself after receiving the packet.

The server is free to stop accepting new connections on a secondary port and release it after the expiry time has been
reached.

# Even More Primitive TCP Chat

## Build instructions

1. Clone the repo to your machine.
2. Run `gradlew shadowJar`.
3. The jar `tcp-chat-all.jar` will be located at `build/libs`.

## Usage instructions

Run `java -jar tcp-chat-all.jar server` to run the server. You don't need to do anything after following
the setup instructions. Use ^C for program exit.

Run `java -jar tcp-chat-all.jar client` to run the client. Write `/file <filename>` to send a file.
Write anything else to send a message. Use ^C for program exit.

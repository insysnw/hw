# Primitive TCP Chat Protocol

[introduction]

## Packets and data conventions

### General data conventions

**UTF-8 character encoding** is implied when it comes to byte representation of any string.

**Big endianness** is implied when it comes to byte representation of any number.

All timestamps are **64-bit** and in **Unix time** (number of seconds since 00:00:00 UTC on 1 January 1970).

All byte representations of client usernames must be of length **between 1 and 127 bytes**.

All byte representations of messages must be of length **up to 2^32 - 128 bytes**.

All file contents must be of length **up to 2^32 - 1 bytes**.

All byte representations of filenames must be of length **between 1 and 2^32 - 132 bytes**.

### General packet structure

```
           | 0               | 1               | 2               | 3               |
           | 0 1 2 3 4 5 6 7 | 0 1 2 3 4 5 6 7 | 0 1 2 3 4 5 6 7 | 0 1 2 3 4 5 6 7 |
 - - - - - |-----------------------------------------------------------------------| - - - - -     ---
         0 | data size (in bytes)                                                  | 3              | h
 - - - - - |-----------------------------------------------------------------------| - - - - -      | e
         4 | version         | type   | event  | x x x x x x x x | x x x x x x x x | 7              | a
 - - - - - |-----------------------------------------------------------------------| - - - - -      | d
         8 | timestamp                                                             | 11             | e
 - - - - - |                                                                       | - - - - -      | r
        12 |                                                                       | 15             |
 - - - - - |-----------------------------------------------------------------------| - - - - -      |
        16 | pointer A (in bytes)                                                  | 19             |
 - - - - - |-----------------------------------------------------------------------| - - - - -      |
        20 | pointer B (in bytes)                                                  | 23             |
 - - - - - |-----------------------------------------------------------------------| - - - - -     ---
        24   client name                                                             23 + ptr A     | d
 - - - - - |- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -| - - - - -      | a
24 + ptr A   value A                                                                 23 + ptr B     | t
 - - - - - |- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -| - - - - -      | a
24 + ptr B   value B                                                                 23 +  size     |
 - - - - - |- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -| - - - - -     ---
```

The first 24 bytes of any packet constitute its header and must always be present. The remaining `data size` bytes is
the packet's data. As one can see, it is variable-length and therefore may be absent altogether. The exact structure and
purpose of any packet is determined by its type (which is expectedly stored in the `type` field).

The `version` field was introduced for the sake of (improbable) major updates of PTCPCP; it is considered equal to 1
across the rest of this document. The `event` field (in combination with the `type` field) is used to further
distinguish different interactions between actors.

The `timestamp` field represents a particular moment in time. Its exact meaning is also governed by the packet's type. 
The `pointer A` and `pointer B` fields are used to locate the first bytes of `value A` and `value B` respectively. Value
pointers do not consider the packet's header; in other words, the reference index is 24. It is required that `pointer A 
<= pointer B` and `pointer B <= data size`.

The `client name` field contains a byte representation of a string representing a client that's connected to the server
— their username. This field does not necessarily represent the packet's sender/receiver.

The `value A` and `value B` fields are byte representations of some objects relevant to the interaction between hosts.
As usual, their actual type and purpose is governed by the packet's type.

### `Connection` packet type

`Connection` packets describe events related to the process of client connecting to the chat server. For all such
packets the value of the field `type` is 0, fields `value A` and `value B` are absent, and fields `pointer A` and
`pointer B` are consequentially equal to `data size`.

PTCPCP defines four variations of the `Connection` packet type:
* `ConnectionNotification` — `event` is equal to 0, the packet is sent from the server to clients, `timestamp`
represents the moment of a client connecting to the chat server, and `client name` represents this client's username.
* `ConnectionRequest` — `event` is equal to 1, the packet is sent from a client to the server, `timestamp` is
typically ignored, `client name` represents the username that the client wishes to use.
* `ConnectionApproved` — `event` is equal to 2, the packet is sent from the server to a client,
`timestamp` is typically ignored, `client name` represents the username that the server has allocated to the client.
* `ConnectionRejected` — `event` is equal to 3, the packet is sent from the server to a client,
`timestamp` and `client name` are typically ignored.

### `DisconnectionNotification` packet

`DisconnectionNotification` packet describes a client's disconnection from the chat server. For all such packets the
value of the field `type` is 1, `event` is equal to 0, fields `value A` and `value B` are absent, fields `pointer A`
and `pointer B` are consequentially equal to `data size`, `timestamp` represents the moment of a client disconnecting
from the chat server, and `client name` represents this client's username. This packet is only sent from the server to
clients.

### `MessageSent` packet

`MessageSent` packet represents a chat message sent by one of connected clients. For all such packets the value of the
field `type` is 2, `event` is equal to 0, field `value B` is absent and field `pointer B` is consequentially equal to
`data size`, and the field `value A` represents a byte representation of the text message being sent.

This packet can be sent in both directions:
* if `MessageSent` is sent from a client to the server, fields `timestamp` and `client name` are typically ignored;
* if `MessageSent` is sent from the server to clients, `timestamp` represents the moment at which the client has
received the message, and `client name` contains the author's username.

### `FileSent` packet

`FileSent` packet represents an actual file being either uploaded or downloaded. For all such packets the value of the
field `type` is 3, `event` is equal to 0, field `value B` is absent and field `pointer B` is consequentially equal to
`data size`, field `client name` is absent and field `pointer A` is consequentially equal to 0, the field `value A`
represents file contents being sent, and `timestamp` is usually ignored. This packet can be sent in both directions.

### `FileTransfer` packet type

`FileTranfer` packets represent information about possible or requested file transfers between the chat server and a
client. For all such packets the value of the field `type` is 4 and `value A` contains the file name.

PTCPCP defines three variations of the `FileTransfer` packet type:
* `FileNotification` — `event` is equal to 0, `client name` contains a username of the file uploader, `timestamp`
represents the moment of the file in question being uploaded to the chat server, and `value B` contains the file size;
this packet is only sent from the server to clients.
* `FileUploadRequest` — `event` is equal to 1, `client name` and `timestamp` are usually ignored, `value B` contains the
file size; this packet is only sent from a client to the server.
* `FileDownloadRequest` —`event` is equal to 2, `timestamp` and `value B` is usually ignored, `client name` contains a
username of the file uploader; this packet is only sent from a client to the server.

### `FileTransferResponse` packet type

`FileTransferResponse` packets are used by the chat server to either initiate file transfer TCP connections or reject
according transfer requests. For all such packets the value of the field `type` is 5, `client name` contains the name of
the file uploader, `value A` contains the file name, and `timestamp` is usually ignored. These packets are only sent
from the server to a client.

PTCPCP defines four variations of the `FileTransferResponse` packet type:
* `FileUploadApproved` — `event` is equal to 0, `value B` contains the socket port to connect to for file transfer.
* `FileUploadRejected` — `event` is equal to 1, `value B` is ignored.
* `FileDownloadApproved` — `event` is equal to 2, `value B` contains the socket port to connect to for file transfer.
* `FileDownloadRejected` — `event` is equal to 3, `value B` is ignored.

### `KeepAlive` packet

`KeepAlive` packets are meant to be used to keep the connection between a client and the server alive. For all such
packets the value of the field `type` is 6, `event` is equal to 0, fields `client name`, `value A`, and `value B` are
absent, fields `data size`, `pointer A`, and `pointer B` are consequentially equal to 0, `timestamp` is usually ignored.

## Interactions

PTCPCP governs six following scenarios of interactions between hosts: 

1. A client connecting to the chat server.
2. A client disconnecting from the chat server.
3. A client sending a text message to chat.
4. A client uploading a file to the chat server.
5. A client downloading a file from the chat server.
6. The chat server keeping track of active client connections.

### Client connection

After establishing a TCP connection to the chat server, the client should send a `ConnectionRequest` packet containing
the username this client wants to be known under to other clients. A valid username should contain at least one
character (purely per common sense considerations), but otherwise is unrestricted.

Upon receiving a `ConnectionRequest` packet containing the client's name suggestion, the server should answer with
either a `ConnectionApproved` packet or a `ConnectionRejected` packet. The only guarantee the server gives when making
the decision is that each client has a unique username at any given time. In all other regards the server is free to
make its own judgments (that do not contradict other points of this document).

The username provided in a `ConnectionApproved` packet may differ from the one asked by the client — this means that the
server identifies the client by a different nickname other than the one asked for. This allows the server to let more
people join the chat while maintaining the username uniqueness guarantee, should one care to implement such feature.

If the server accepted the connection by sending a `ConnectionApproved` packet, it should also notify all other clients
about a new client joining the chat by sending everyone an appropriate `ConnectionNotification` packet.

The client can only expect further communication with the server if it has received a `ConnectionApproved` packet. The
server is also free to close the TCP connection after sending a `ConnectionRejected` packet.

### Client disconnection

A client initiates the disconnection by simply closing the TCP connection. The server should notify every other client
about such disconnection with a `DisconnectionNotification` packet per each fully connected client being disconnected.

### Message transfer

To send the message to the chat, the client should send a valid `MessageSent` packet to the chat server. The server will
forward the message contained in this packet to other clients via another `MessageSent` packet. The author of the
message should not receive the copy of the message he sent.

### File upload

To initiate a file upload, the client should send a valid `FileUploadRequest` packet to the server. The server should
then decide whether it can accept the file or not. The server should provide a guarantee that the file will never be
overwritten. In order to compensate for this, the uploader's username is also used to identify the file during and after
the upload, so the server's file namespace is not polluted too much. Note that having the same username as the uploader
is not grounds for overwriting the file.

In the case the file is accepted for upload, the server prepares itself for another TCP connection with the client and 
sends a `FileUploadAccepted` to the client, providing a socket port for starting said TCP connection. The client can
expect that the host name has remained the same when initiating this additional connection.

After a file transfer TCP connection is established, the client should send a `FileSent` packet to the server, after
which the connection may be closed. Upon successfully receiving the file, the server should send a `FileNotification`
packet to all connected clients (including the uploader (unlike the message transfer situation)).

If the server deems the file to be unacceptable, it sends a `FileUploadRejection` packet to the uploader.

It should be noted that a `file size` value provided in a `FileUploadRequest` packet is not representative of an actual
file's size, and therefore should only be used as a reference for the initial decision. This is a rather severe design
miscalculation that should probably be fixed in another version of the protocol, if one should ever be created.

### File download

A procedure for a file download is similar to that of an upload. The client should send a valid `FileDownloadRequest`
packet to the server. If the server is able to provide a requested file (which is identified by the uploader's username
and the file name itself), it sends a `FileDownloadAccepted` packet to the client to initiate a separate TCP connection.
Using this separate connection, the server sends a `FileSent` packet to the client, after which the connection may be
closed. If the file is unavailable, the server should send a `FileDownloadRejected` packet.

### Connection tracking

In order to be able to disconnect troubled clients from the server, the server imposes a restriction on all clients:
a packet should be received by the server in 2 seconds since the last one if the client wants to expect the connection
to be kept alive. If this requirement is not met, the server is free to close the TCP connection. A separate `KeepAlive`
packet is available for meeting this requirement, although any packet should counts.

### General considerations

The `client name` and `timestamp` values provided by a client should usually not be forwarded to other clients by the
server. The server should instead decide the replacements for these values itself based on TCP connection identification
and its own time. Client-provided values should only be used for internal purposes inside the server, if one so wishes.

### Closing remarks

This protocol is a mess.

--------

# Primitive TCP Chat

[introduction]

## Build instructions

1. Clone the repo to your machine.
2. `gradlew shadowJar`.
3. The jar `tcp-chat-all.jar` will be located at `build/libs`.

## Usage instructions

Run `java -jar tcp-chat-all.jar server` to run the server.

Run `java -jar tcp-chat-all.jar client` to run the client.

Pass `--no-keep-alive` to the client as command-line argument to disable the keep-alive system (and to be disconnected
in approximately two seconds).

Pass `--keep-old-files` to the server as command-line argument  or to the client to prevent it from running if there are
any files at the file storage location (`./files`).

## Examples

[placeholder]

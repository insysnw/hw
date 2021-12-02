# Primitive TCP Chat Protocol

introduction

## Packets and data conventions

### Data conventions

placeholder

### General packet structure

```
   | 0                       | 1                       | 2                       | 3                       |
   | 0  1  2  3   4  5  6  7 | 0  1  2  3   4  5  6  7 | 0  1  2  3   4  5  6  7 | 0  1  2  3   4  5  6  7 |
---|-------------------------------------------------------------------------------------------------------|
 0 | data length (in bytes)                                                                                |
   |                                                                                                       |
---|-------------------------------------------------------------------------------------------------------|
 4 | protocol version        | packet     | state      | padding / reserved                                |
   |                         | type       |            |                                                   |
---|-------------------------------------------------------------------------------------------------------|
 8 | timestamp                                                                                             |
   |                                                                                                       |
---|                                                                                                       |
12 |                                                                                                       |
   |                                                                                                       |
---|-------------------------------------------------------------------------------------------------------|
16 | pointer 1                                                                                             |
   |                                                                                                       |
---|-------------------------------------------------------------------------------------------------------|
20 | pointer 2                                                                                             |
   |                                                                                                       |
---|-------------------------------------------------------------------------------------------------------|
24 | client name                                                                                           |

---|- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -|
 X | value 1                                                                                               |

---|- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -|
 Y | value 2                                                                                               |

---|- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -|
```

placeholder

### `Connection` packet

placeholder

### `Disconnection` packet

placeholder

### `Message` packet

placeholder

### `File` packet

placeholder

### `FileTransfer` packet

placeholder

### `FileTransferResponse` packet

placeholder

### `KeepAlive` packet

placeholder

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
either ...

### Client disconnection

placeholder

### Message transfer

placeholder

### File upload

placeholder

### File download

placeholder

### Connection tracking

placeholder

--------

# Primitive TCP Chat

introduction

## Build instructions

placeholder

## Examples

placeholder

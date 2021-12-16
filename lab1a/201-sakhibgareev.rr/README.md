# Repo
https://github.com/rapturemain/tcp-messenger-client/tree/release_21-1
https://github.com/rapturemain/tcp-messenger-message-framework/tree/release_21-1
https://github.com/rapturemain/tcp-messenger-server/tree/master

# Protocol description
For all messages you may and should not fill `timestamp` and `senderName` fields.

### User registration and disconnection
After TCP connection you need to
1. Register
2. Reset connection

To register just send
1. Send `RegistrationRequestMessage` with username
2. Receive `RegistrationResponseMessage` with successful registration
   1. If registration is not successful, try to use another credentials

To reset connection you can simply reset TCP connection or also send `ConnectionResetMessage`. 
Be aware, that server can also disconnect you. In this case it will send the same message to you 
before TCP connection reset.

Also, upon user connection and disconnection you will receive `UserConnectedMessage`, `UserDisconnectedMessage`
and `UserRegisteredMessage`

You do not need to register to receive users chat messages, but you need to register in order to send them.

### Chat messages
There are 3 types of chat messages:
1. `SimpleChatMessage`, containing text, sender name and timestamp.
2. `FileChatMessage`, containing file as a byte array, its name, sender name and timestamp

### Messages byte format
Description provided by `tcp-messenger-message-framework` repo. See its README
package net.fennmata.cnt.lab1.common

sealed interface PacketState { val value: Int }

sealed class ConnectionState(override val value: Int) : PacketState
object ConnectionNotification : ConnectionState(0)
object ConnectionRequest : ConnectionState(1)
object ConnectionApproved : ConnectionState(2)
object ConnectionRejected : ConnectionState(3)

val connectionStates = listOf(
    ConnectionNotification,
    ConnectionRequest,
    ConnectionApproved,
    ConnectionRejected
)

sealed class DisconnectionState(override val value: Int) : PacketState
object DisconnectionNotification : DisconnectionState(0)

val disconnectionStates = listOf(DisconnectionNotification)

sealed class MessageState(override val value: Int) : PacketState
object MessageSent : MessageState(0)

val messageStates = listOf(MessageSent)

sealed class FileState(override val value: Int) : PacketState
object FileSent : FileState(0)

val fileStates = listOf(FileSent)

sealed class FileTransferState(override val value: Int) : PacketState
object FileNotification : FileTransferState(0)
object FileUploadRequest : FileTransferState(1)
object FileDownloadRequest : FileTransferState(2)

val fileTransferStates = listOf(
    FileNotification,
    FileUploadRequest,
    FileDownloadRequest
)

sealed class FileTransferResponseState(override val value: Int) : PacketState
object FileUploadApproved : FileTransferResponseState(0)
object FileUploadRejected : FileTransferResponseState(1)
object FileDownloadApproved : FileTransferResponseState(2)
object FileDownloadRejected : FileTransferResponseState(3)

val fileTransferResponseStates = listOf(
    FileUploadApproved,
    FileUploadRejected,
    FileDownloadApproved,
    FileDownloadRejected
)

sealed class KeepAliveState(override val value: Int) : PacketState
object KeepAlivePing : KeepAliveState(0)

val keepAliveStates = listOf(KeepAlivePing)

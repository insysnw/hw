package net.fennmata.cnt.lab1.common

sealed interface PacketState {
    val value: Int
}

sealed class ConnectionState(override val value: Int) : PacketState
object ConnectionNotification : ConnectionState(0)
object ConnectionRequest : ConnectionState(1)
object ConnectionApproved : ConnectionState(2)
object ConnectionRejected : ConnectionState(3)

val connectionStates = listOf(
    ConnectionRequest,
    ConnectionNotification,
    ConnectionApproved,
    ConnectionRejected
)

sealed class DisconnectionState(override val value: Int) : PacketState
object DisconnectionNotification : DisconnectionState(0)

val disconnectionStates = listOf(
    DisconnectionNotification
)

sealed class MessageState(override val value: Int) : PacketState
object MessageSent : MessageState(0)

val messageStates = listOf(
    MessageSent
)

sealed class FileState(override val value: Int) : PacketState
object FileNotification : FileState(0)
object FileUploadRequest : FileState(1)
object FileDownloadRequest : FileState(2)

val fileStates = listOf(
    FileNotification,
    FileUploadRequest,
    FileDownloadRequest
)

sealed class FileTransferInfoState(override val value: Int) : PacketState
object FileUploadApproved : FileTransferInfoState(0)
object FileUploadRejected : FileTransferInfoState(1)
object FileDownloadApproved : FileTransferInfoState(2)
object FileDownloadRejected : FileTransferInfoState(3)

val fileTransferInfoStates = listOf(
    FileUploadApproved,
    FileUploadRejected,
    FileDownloadApproved,
    FileDownloadRejected
)

sealed class FileTransferState(override val value: Int) : PacketState
object FileTransfer : FileTransferState(0)

val fileTransferStates = listOf(
    FileTransfer
)

sealed class KeepAliveState(override val value: Int) : PacketState
object KeepAlivePing : KeepAliveState(0)

val keepAliveStates = listOf(
    KeepAlivePing
)

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

val messageStates = listOf<MessageState>(
    MessageSent
)

sealed class FileState(override val value: Int) : PacketState
// TODO

val fileStates = listOf<FileState>(
    // TODO
)

sealed class KeepAliveState(override val value: Int) : PacketState
object KeepAlivePing : KeepAliveState(0)

val keepAliveStates = listOf(
    KeepAlivePing
)

package net.fennmata.cnt.lab1.common

sealed interface PacketState {
    val value: Int
}

sealed class ConnectionState(override val value: Int) : PacketState
object ConnectionRequest : ConnectionState(0)
object ConnectionNotification : ConnectionState(1)
object ConnectionApproved : ConnectionState(2)
object ConnectionRejected : ConnectionState(3)

val connectionStates = listOf(
    ConnectionRequest,
    ConnectionNotification,
    ConnectionApproved,
    ConnectionRejected
)

sealed class DisconnectionState(override val value: Int) : PacketState
object DisconnectionRequest : ConnectionState(0)
object DisconnectionNotification : ConnectionState(1)

val disconnectionStates = listOf(
    DisconnectionRequest,
    DisconnectionNotification
)

sealed class MessageState(override val value: Int) : PacketState
// TODO

val messageStates = listOf<MessageState>(
    // TODO
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

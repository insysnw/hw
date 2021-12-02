package net.fennmata.cnt.lab1.common

import java.nio.ByteBuffer
import java.time.OffsetDateTime

sealed class Packet<T : PacketState>(val state: T, val timestamp: OffsetDateTime) {
    abstract val clientName: String
    abstract val object1: ByteArray
    abstract val object2: ByteArray

    val dataLength by lazy { clientName.length + object1.size + object2.size }
}

class ConnectionPacket(
    state: ConnectionState,
    timestamp: OffsetDateTime,
    override val clientName: String
) : Packet<ConnectionState>(state, timestamp) {
    override val object1 = byteArrayOf()
    override val object2 = byteArrayOf()
}

class DisconnectionPacket(
    state: DisconnectionState,
    timestamp: OffsetDateTime,
    override val clientName: String
) : Packet<DisconnectionState>(state, timestamp) {
    override val object1 = byteArrayOf()
    override val object2 = byteArrayOf()
}

class MessagePacket(
    state: MessageState,
    timestamp: OffsetDateTime,
    override val clientName: String,
    val message: String
) : Packet<MessageState>(state, timestamp) {
    override val object1 = message.toByteArray()
    override val object2 = byteArrayOf()
}

class FilePacket(
    state: FileState,
    timestamp: OffsetDateTime,
    override val clientName: String,
    val fileContents: ByteArray
) : Packet<FileState>(state, timestamp) {
    override val object1 = fileContents
    override val object2 = byteArrayOf()
}

class FileTransferPacket(
    state: FileTransferState,
    timestamp: OffsetDateTime,
    override val clientName: String,
    val fileName: String,
    val fileLength: Int
) : Packet<FileTransferState>(state, timestamp) {
    override val object1 = fileName.toByteArray()
    override val object2: ByteArray = ByteBuffer.allocate(4).putInt(fileLength).array()
}

class FileTransferResponsePacket(
    state: FileTransferResponseState,
    timestamp: OffsetDateTime,
    override val clientName: String,
    val fileName: String,
    val socketPort: Int
) : Packet<FileTransferResponseState>(state, timestamp) {
    override val object1 = fileName.toByteArray()
    override val object2: ByteArray = ByteBuffer.allocate(4).putInt(socketPort).array()
}

class KeepAlivePacket(
    state: KeepAliveState,
    timestamp: OffsetDateTime
) : Packet<KeepAliveState>(state, timestamp) {
    override val clientName = ""
    override val object1 = byteArrayOf()
    override val object2 = byteArrayOf()
}

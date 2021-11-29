package net.fennmata.cnt.lab1.common

import java.nio.ByteBuffer
import java.time.OffsetDateTime

sealed class Packet(val state: PacketState, val timestamp: OffsetDateTime) {
    abstract val clientName: String
    abstract val object1: ByteArray
    abstract val object2: ByteArray

    val dataLength by lazy { clientName.length + object1.size + object2.size }
}

class ConnectionPacket(
    state: ConnectionState,
    timestamp: OffsetDateTime,
    override val clientName: String
) : Packet(state, timestamp) {
    override val object1 = byteArrayOf()
    override val object2 = byteArrayOf()
}

class DisconnectionPacket(
    state: DisconnectionState,
    timestamp: OffsetDateTime,
    override val clientName: String
) : Packet(state, timestamp) {
    override val object1 = byteArrayOf()
    override val object2 = byteArrayOf()
}

class MessagePacket(
    state: MessageState,
    timestamp: OffsetDateTime,
    override val clientName: String,
    val message: String
) : Packet(state, timestamp) {
    override val object1 = message.toByteArray()
    override val object2 = byteArrayOf()
}

class FilePacket(
    state: FileState,
    timestamp: OffsetDateTime,
    override val clientName: String,
    val fileName: String,
    val fileLength: Long
) : Packet(state, timestamp) {
    override val object1 = fileName.toByteArray()
    override val object2: ByteArray = ByteBuffer.allocate(8).putLong(fileLength).array()
}

class KeepAlivePacket(
    state: KeepAliveState,
    timestamp: OffsetDateTime
) : Packet(state, timestamp) {
    override val clientName = ""
    override val object1 = byteArrayOf()
    override val object2 = byteArrayOf()
}

package net.fennmata.cnt.lab1.common

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

fun InputStream.readPacket(): Packet? {
    val header = readNBytes(16)
    if (header.size < 16) throw IOException("Stream closed before a full packet was read")
    val dataLength = header.slice(2..3).toIntWithoutSign()
    val data = readNBytes(dataLength)
    if (data.size < dataLength) throw IOException("Stream closed before a full packet was read")

    val version = header[0].toIntWithoutSign()
    if (version != 1) return null

    val kind = header[1].toIntWithoutSign()
    val (typeValue, stateValue) = ((kind shr 4) and 0xF) to (kind and 0xF)
    val state = getStatesByType(typeValue)?.getStateByValue(stateValue) ?: return null

    val timestampSeconds = header.slice(4..11).toLongWithoutSign()
    val timestampInstant = Instant.ofEpochSecond(timestampSeconds)
    val timestamp = OffsetDateTime.ofInstant(timestampInstant, ZoneId.systemDefault())

    val pointer1 = header.slice(12..13).toIntWithoutSign()
    val pointer2 = header.slice(14..15).toIntWithoutSign()

    if (pointer1 > pointer2 || pointer2 > dataLength) return null
    val clientName = data.decodeToString(0, pointer1)
    val object1 = data.sliceArray(pointer1 until pointer2)
    val object2 = data.sliceArray(pointer2 until dataLength)

    return when (state) {
        is ConnectionState -> ConnectionPacket(state, timestamp, clientName)
        is DisconnectionState -> DisconnectionPacket(state, timestamp, clientName)
        is MessageState -> MessagePacket(
            state, timestamp, clientName, object1.decodeToString()
        )
        is FileState -> FilePacket(
            state, timestamp, clientName, object1.decodeToString(), object2.toLongWithoutSign()
        )
        is KeepAliveState -> KeepAlivePacket(state, timestamp)
    }
}

fun OutputStream.writePacket(packet: Packet) {
    val buffer: ByteBuffer
    with(packet) {
        buffer = ByteBuffer.allocate(16 + dataLength)
        buffer.put(1) // version
        buffer.put(((typeValue shl 4) or state.value).toByte()) // type + state
        buffer.putShort(dataLength.toShort())
        buffer.putLong(timestamp.toEpochSecond())
        buffer.putShort(clientName.length.toShort()) // pointer1
        buffer.putShort((clientName.length + object1.size).toShort()) // pointer2
        buffer.put(clientName.toByteArray())
        buffer.put(object1)
        buffer.put(object2)
    }
    write(buffer.array())
}

fun Socket.readPacket(): Packet? {
    if (isClosed) throw SocketException("Socket closed")
    return getInputStream().use { inputStream ->
        try {
            inputStream.readPacket()
        } catch (e: IOException) {
            throw SocketException(e.message)
        }
    }
}

fun Socket.writePacket(packet: Packet) {
    if (isClosed) throw SocketException("Socket closed")
    getOutputStream().use { outputStream ->
        try {
            outputStream.writePacket(packet)
        } catch (e: IOException) {
            throw SocketException(e.message)
        }
    }
}

private fun List<Byte>.toNumberWithoutSign(): Long {
    return map { it.toLong() and 0xFF }.fold(0L) { accumulator, next -> (accumulator shl 8) or next }
}

private fun List<Byte>.toLongWithoutSign(): Long = toNumberWithoutSign()

private fun List<Byte>.toIntWithoutSign(): Int = toNumberWithoutSign().toInt()

private fun ByteArray.toLongWithoutSign(): Long = toList().toLongWithoutSign()

private fun ByteArray.toIntWithoutSign(): Int = toList().toIntWithoutSign()

private fun Byte.toLongWithoutSign(): Long = listOf(this).toLongWithoutSign()

private fun Byte.toIntWithoutSign(): Int = listOf(this).toIntWithoutSign()

private enum class PacketType(val packetValue: Int) {
    CONNECTION(0),
    DISCONNECTION(1),
    MESSAGE(2),
    FILE(3),
    KEEPALIVE(4)
}

private val PacketState.typeValue get() = when (this) {
    is ConnectionState -> PacketType.CONNECTION.packetValue
    is DisconnectionState -> PacketType.DISCONNECTION.packetValue
    is MessageState -> PacketType.MESSAGE.packetValue
    is FileState -> PacketType.FILE.packetValue
    is KeepAliveState -> PacketType.KEEPALIVE.packetValue
}

private val Packet.typeValue get() = state.typeValue

private fun getStatesByType(typeValue: Int) = when (typeValue) {
    PacketType.CONNECTION.packetValue -> connectionStates
    PacketType.DISCONNECTION.packetValue -> disconnectionStates
    PacketType.MESSAGE.packetValue -> messageStates
    PacketType.FILE.packetValue -> fileStates
    PacketType.KEEPALIVE.packetValue -> keepAliveStates
    else -> null
}

private fun List<PacketState>.getStateByValue(stateValue: Int) = find { it.value == stateValue }

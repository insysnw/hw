package net.fennmata.cnt.lab1.common

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException

fun InputStream.readPacket(): Packet {
    val protocolVersion = readNBytes(1)
    if (protocolVersion.isEmpty()) throw IOException("Stream closed before a full packet was read")
    check(protocolVersion.first() == 1.toByte()) { "Packet with an illegal protocol version received" }

    val supposedEvent = readNBytes(1)
    if (protocolVersion.isEmpty()) throw IOException("Stream closed before a full packet was read")
    val event = supposedEvent.first().toPacketEvent()
    checkNotNull(event) { "Packet with an illegal event received" }

    val bodyConstsWidth = event.constantBodyPartWidth
    val bodyConsts = readNBytes(bodyConstsWidth)
    if (bodyConsts.size < bodyConstsWidth) throw IOException("Stream closed before a full packet was read")

    val bodyVarsWidth = event.calculateVariableBodyPartWidth(bodyConsts)
    val bodyVars = readNBytes(bodyVarsWidth)
    if (bodyVars.size < bodyVarsWidth) throw IOException("Stream closed before a full packet was read")

    return event.compilePacket(bodyConsts, bodyVars)
}

fun OutputStream.writePacket(packet: Packet) = write(packet.toByteArray())

fun Socket.readPacket(): Packet {
    if (isClosed) throw SocketException("Socket closed")
    return try {
        getInputStream().readPacket()
    } catch (e: Exception) {
        close()
        throw SocketException(e.message)
    }
}

fun Socket.writePacket(packet: Packet) {
    if (isClosed) throw SocketException("Socket closed")
    try {
        getOutputStream().writePacket(packet)
    } catch (e: Exception) {
        close()
        throw SocketException(e.message)
    }
}

fun Socket.readPacketSafely(doIfReadFailed: (SocketException) -> Unit): Packet? {
    return try {
        readPacket()
    } catch (e: SocketException) {
        doIfReadFailed(e)
        null
    }
}

fun Socket.writePacketSafely(packet: Packet, doIfWriteFailed: (SocketException) -> Unit) {
    try {
        writePacket(packet)
    } catch (e: SocketException) {
        doIfWriteFailed(e)
    }
}

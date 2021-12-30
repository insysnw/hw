package net.fennmata.cnt.lab1.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.OffsetDateTime

class PacketTests {

    @Test
    fun incorrectVersionTest() {
        val leftovers = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

        val input = byteArrayOf(0) + leftovers
        val buffer = PacketBuffer()
        buffer.put(input)

        assertEquals(PacketBuffer.State.PACKET_INCORRECT, buffer.state)
        assertThrows(IllegalStateException::class.java) { buffer.toPacket() }
        assertArrayEquals(leftovers, buffer.leftoverBytes)
        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
    }

    @Test
    fun nonexistentEventTest() {
        val leftovers = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

        val input = byteArrayOf(1, 16) + leftovers
        val buffer = PacketBuffer()
        buffer.put(input)

        assertEquals(PacketBuffer.State.PACKET_INCORRECT, buffer.state)
        assertThrows(IllegalStateException::class.java) { buffer.toPacket() }
        assertArrayEquals(leftovers, buffer.leftoverBytes)
        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "user", "user name !!", "ⓤⓢⓔⓡⓝⓐⓜⓔ"])
    fun connectionRequestPacketTest(username: String) {
        val packet = ConnectionRequest(username)

        val input = ConnectionRequest(username).toByteArray()
        val buffer = PacketBuffer()
        buffer.put(input)

        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
        assertEquals(packet, buffer.toPacket())
        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
    }

    @Test
    fun connectionAcceptedPacketTest() {
        val packet = ConnectionAccepted

        val input = packet.toByteArray()
        val buffer = PacketBuffer()
        buffer.put(input)

        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
        assertEquals(packet, buffer.toPacket())
        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "user", "user name !!", "ⓤⓢⓔⓡⓝⓐⓜⓔ"])
    fun connectionNotificationPacketTest(username: String) {
        val timestamp = OffsetDateTime.now().withNano(0)
        val packet = ConnectionNotification(timestamp, username)

        val input = packet.toByteArray()
        val buffer = PacketBuffer()
        buffer.put(input)

        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
        assertEquals(packet, buffer.toPacket())
        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
    }

    @Test
    fun connectionRejectedPacketTest() {
        val leftovers = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val packet = ConnectionRejected

        val input = packet.toByteArray() + leftovers
        val buffer = PacketBuffer()
        buffer.put(input)

        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
        assertEquals(packet, buffer.toPacket())
        assertArrayEquals(leftovers, buffer.leftoverBytes)
        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "user", "user name !!", "ⓤⓢⓔⓡⓝⓐⓜⓔ"])
    fun disconnectionNotificationPacketTest(username: String) {
        val timestamp = OffsetDateTime.now().withNano(0)
        val packet = DisconnectionNotification(timestamp, username)

        val input = packet.toByteArray()
        val buffer = PacketBuffer()
        buffer.put(input)

        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
        assertEquals(packet, buffer.toPacket())
        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Hi!", "Hello there!\nI hate you!"])
    fun messageSentPacketTest(message: String) {
        val packet = MessageSent(message)

        val input = packet.toByteArray()
        val buffer = PacketBuffer()
        buffer.put(input)

        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
        assertEquals(packet, buffer.toPacket())
        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Hi!", "Hello there!\nI hate you!"])
    fun messageNotificationPacketTest(message: String) {
        val timestamp = OffsetDateTime.now().withNano(0)
        val username = "ⓤⓢⓔⓡⓝⓐⓜⓔ"
        val packet = MessageNotification(timestamp, username, message)

        val input = packet.toByteArray()
        val buffer = PacketBuffer()
        buffer.put(input)

        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
        assertEquals(packet, buffer.toPacket())
        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
    }

    @Test
    fun incorrectUsernameTest() {
        val timestamp = OffsetDateTime.now().withNano(0)
        val message = "duh"
        val validUsername = "A".repeat(0xFF)
        val incorrectUsername = validUsername + "A"

        assertDoesNotThrow { ConnectionRequest(validUsername) }
        assertDoesNotThrow { ConnectionNotification(timestamp, validUsername) }
        assertDoesNotThrow { DisconnectionNotification(timestamp, validUsername) }
        assertDoesNotThrow { MessageNotification(timestamp, validUsername, message) }

        assertThrows(IllegalStateException::class.java) { ConnectionRequest(incorrectUsername) }
        assertThrows(IllegalStateException::class.java) { ConnectionNotification(timestamp, incorrectUsername) }
        assertThrows(IllegalStateException::class.java) { DisconnectionNotification(timestamp, incorrectUsername) }
        assertThrows(IllegalStateException::class.java) { MessageNotification(timestamp, incorrectUsername, message) }
    }

    @Test
    fun incorrectMessageTest() {
        val timestamp = OffsetDateTime.now().withNano(0)
        val username = "ⓤⓢⓔⓡⓝⓐⓜⓔ"
        val validMessage = "A".repeat(0xFFFF)
        val incorrectMessage = validMessage + "A"

        assertDoesNotThrow { MessageSent(validMessage) }
        assertDoesNotThrow { MessageNotification(timestamp, username, validMessage) }

        assertThrows(IllegalStateException::class.java) { MessageSent(incorrectMessage) }
        assertThrows(IllegalStateException::class.java) { MessageNotification(timestamp, username, incorrectMessage) }
    }

}

package net.fennmata.cnt.lab1.common

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.OffsetDateTime
import java.util.stream.Stream

class PacketBufferTests {

    private fun checkIncorrectness(bytes: ByteArray) {
        val leftovers = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val addition = byteArrayOf(0, 4, 8)

        val buffer = PacketBuffer()
        buffer.put(bytes + leftovers)

        assertEquals(PacketBuffer.State.PACKET_INCORRECT, buffer.state)
        assertThrows(IllegalStateException::class.java) { buffer.toPacket() }
        assertArrayEquals(leftovers, buffer.leftoverBytes)
        assertThrows(IllegalStateException::class.java) { buffer.put(addition) }
    }

    private fun checkCorrectness(packet: Packet) {
        val leftovers = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val addition = byteArrayOf(0, 4, 8)

        val buffer = PacketBuffer()
        buffer.put(packet.toByteArray() + leftovers)

        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
        assertEquals(packet, buffer.toPacket())
        assertArrayEquals(leftovers, buffer.leftoverBytes)
        assertThrows(IllegalStateException::class.java) { buffer.put(addition) }
    }

    @Test
    fun incorrectVersionTest() = checkIncorrectness(byteArrayOf(0))

    @Test
    fun nonexistentEventTest() = checkIncorrectness(byteArrayOf(1, 16))

    @ParameterizedTest
    @MethodSource("connectionRequestTestSource")
    fun connectionRequestTest(packet: ConnectionRequest) = checkCorrectness(packet)

    @Test
    fun connectionAcceptedTest() = checkCorrectness(ConnectionAccepted)

    @ParameterizedTest
    @MethodSource("connectionNotificationTestSource")
    fun connectionNotificationTest(packet: ConnectionNotification) = checkCorrectness(packet)

    @Test
    fun connectionRejectedTest() = checkCorrectness(ConnectionRejected)

    @ParameterizedTest
    @MethodSource("disconnectionNotificationTestSource")
    fun disconnectionNotificationTest(packet: DisconnectionNotification) = checkCorrectness(packet)

    @ParameterizedTest
    @MethodSource("messageSentTestSource")
    fun messageSentTest(packet: MessageSent) = checkCorrectness(packet)

    @ParameterizedTest
    @MethodSource("messageNotificationTestSource")
    fun messageNotificationTest(packet: MessageNotification) = checkCorrectness(packet)

    @ParameterizedTest
    @MethodSource("fileSentTestSource")
    fun fileSentTest(packet: FileSent) = checkCorrectness(packet)

    @ParameterizedTest
    @MethodSource("fileNotificationPendingTestSource")
    fun fileNotificationPendingTest(packet: FileNotificationPending) = checkCorrectness(packet)

    @ParameterizedTest
    @MethodSource("fileNotificationTestSource")
    fun fileNotificationTest(packet: FileNotification) = checkCorrectness(packet)

    // TODO test if PacketBuffer works correctly with a packet arriving in separate segments

    companion object {

        @JvmStatic
        fun connectionRequestTestSource(): Stream<ConnectionRequest> {
            val usernames = listOf("", "user", "user name !!", "ⓤⓢⓔⓡⓝⓐⓜⓔ")
            return usernames.map { ConnectionRequest(it) }.stream()
        }

        @JvmStatic
        fun connectionNotificationTestSource(): Stream<ConnectionNotification> {
            val timestamp = OffsetDateTime.now().withNano(0)
            val usernames = listOf("", "user", "user name !!", "ⓤⓢⓔⓡⓝⓐⓜⓔ")
            return usernames.map { ConnectionNotification(timestamp, it) }.stream()
        }

        @JvmStatic
        fun disconnectionNotificationTestSource(): Stream<DisconnectionNotification> {
            val timestamp = OffsetDateTime.now().withNano(0)
            val usernames = listOf("", "user", "user name !!", "ⓤⓢⓔⓡⓝⓐⓜⓔ")
            return usernames.map { DisconnectionNotification(timestamp, it) }.stream()
        }

        @JvmStatic
        fun messageSentTestSource(): Stream<MessageSent> {
            val messages = listOf("", "Hi!", "Hello there!\nI hate you!", "ⓐ".repeat(10000))
            return messages.map { MessageSent(it) }.stream()
        }

        @JvmStatic
        fun messageNotificationTestSource(): Stream<MessageNotification> = Stream.of(
            MessageNotification(
                OffsetDateTime.now().withNano(0),
                "ⓤⓢⓔⓡⓝⓐⓜⓔ",
                "ⓐ".repeat(10000)
            )
        )

        @JvmStatic
        fun fileSentTestSource(): Stream<FileSent> {
            val contents = listOf(
                "" to "".encodeToByteArray().toList(),
                "text.txt" to "this is a file text".encodeToByteArray().toList(),
                "ⓐ.txt" to "ⓐ".repeat(10000).encodeToByteArray().toList()
            )
            return contents.map { (filename, file) -> FileSent(filename, file) }.stream()
        }

        @JvmStatic
        fun fileNotificationPendingTestSource(): Stream<FileNotificationPending> {
            val expiryTime = OffsetDateTime.now().withNano(0)
            val socketPorts = listOf(1024, 2354, 6456, 32453)
            return socketPorts.map { FileNotificationPending(it, expiryTime) }.stream()
        }

        @JvmStatic
        fun fileNotificationTestSource(): Stream<FileNotification> = Stream.of(
            FileNotification(
                OffsetDateTime.now().withNano(0),
                "ⓤⓢⓔⓡⓝⓐⓜⓔ",
                "ⓐ.txt",
                "ⓐ".repeat(10000).encodeToByteArray().toList()
            )
        )

    }

}

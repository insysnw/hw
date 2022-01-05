package net.fennmata.cnt.lab1.common

//import org.junit.jupiter.api.Assertions.assertArrayEquals
//import org.junit.jupiter.api.Assertions.assertDoesNotThrow
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Assertions.assertThrows
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.params.ParameterizedTest
//import org.junit.jupiter.params.provider.Arguments
//import org.junit.jupiter.params.provider.Arguments.arguments
//import org.junit.jupiter.params.provider.MethodSource
//import org.junit.jupiter.params.provider.ValueSource
//import java.time.OffsetDateTime
//import java.util.stream.Stream
//
//class PacketTestsOld {
//

//
//    @ParameterizedTest
//    @ValueSource(strings = ["", "user", "user name !!", "ⓤⓢⓔⓡⓝⓐⓜⓔ"])
//    fun connectionRequestPacketTest(username: String) {
//        val packet = ConnectionRequest(username)
//
//        val input = ConnectionRequest(username).toByteArray()
//        val buffer = PacketBuffer()
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @Test
//    fun connectionAcceptedPacketTest() {
//        val packet = ConnectionAccepted
//
//        val input = packet.toByteArray()
//        val buffer = PacketBuffer()
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = ["", "user", "user name !!", "ⓤⓢⓔⓡⓝⓐⓜⓔ"])
//    fun connectionNotificationPacketTest(username: String) {
//        val timestamp = OffsetDateTime.now().withNano(0)
//        val packet = ConnectionNotification(timestamp, username)
//
//        val input = packet.toByteArray()
//        val buffer = PacketBuffer()
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @Test
//    fun connectionRejectedPacketTest() {
//        val leftovers = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
//        val packet = ConnectionRejected
//
//        val input = packet.toByteArray() + leftovers
//        val buffer = PacketBuffer()
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(leftovers, buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = ["", "user", "user name !!", "ⓤⓢⓔⓡⓝⓐⓜⓔ"])
//    fun disconnectionNotificationPacketTest(username: String) {
//        val timestamp = OffsetDateTime.now().withNano(0)
//        val packet = DisconnectionNotification(timestamp, username)
//
//        val input = packet.toByteArray()
//        val buffer = PacketBuffer()
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = ["", "Hi!", "Hello there!\nI hate you!"])
//    fun messageSentPacketTest(message: String) {
//        val packet = MessageSent(message)
//
//        val input = packet.toByteArray()
//        val buffer = PacketBuffer()
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = ["", "Hi!", "Hello there!\nI hate you!"])
//    fun messageNotificationPacketTest(message: String) {
//        val timestamp = OffsetDateTime.now().withNano(0)
//        val username = "ⓤⓢⓔⓡⓝⓐⓜⓔ"
//        val packet = MessageNotification(timestamp, username, message)
//
//        val input = packet.toByteArray()
//        val buffer = PacketBuffer()
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @ParameterizedTest
//    @MethodSource("fileUploadPacketSource")
//    fun fileUploadPacketTest(fileId: Int, fileSizeInfo: Int, fileExtension: String, isValid: Boolean) {
//        if (!isValid) {
//            assertThrows(IllegalStateException::class.java) { FileUpload(fileId, fileSizeInfo, fileExtension) }
//            return
//        }
//
//        val packet = FileUpload(fileId, fileSizeInfo, fileExtension)
//
//        val input = packet.toByteArray()
//        val buffer = PacketBuffer()
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @ParameterizedTest
//    @MethodSource("fileUploadAcceptedPacketSource")
//    fun fileUploadAcceptedPacketTest(fileId: Int, socketPort: Int, isValid: Boolean) {
//        if (!isValid) {
//            assertThrows(IllegalStateException::class.java) { FileUploadAccepted(fileId, socketPort) }
//            return
//        }
//
//        val packet = FileUploadAccepted(fileId, socketPort)
//
//        val input = packet.toByteArray()
//        val buffer = PacketBuffer()
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @Test
//    fun fileUploadedPacketTest() {
//        val leftovers = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
//        val packet = FileUploaded("loremipsum".repeat(1024).encodeToByteArray())
//
//        val input = packet.toByteArray() + leftovers
//        val buffer = PacketBuffer(FileSize to 10 * 1024)
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(leftovers, buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = ["", "a.txt", "bcdefg.png"])
//    fun fileNotificationPacketTest(filename: String) {
//        val timestamp = OffsetDateTime.now().withNano(0)
//        val fileSizeInfo = 256
//        val username = "ⓤⓢⓔⓡⓝⓐⓜⓔ"
//        val packet = FileNotification(timestamp, fileSizeInfo, username, filename)
//
//        val input = packet.toByteArray()
//        val buffer = PacketBuffer()
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @ParameterizedTest
//    @MethodSource("fileUploadRejectedPacketSource")
//    fun fileUploadRejectedPacketTest(fileId: Int, isValid: Boolean) {
//        if (!isValid) {
//            assertThrows(IllegalStateException::class.java) { FileUploadRejected(fileId) }
//            return
//        }
//
//        val packet = FileUploadRejected(fileId)
//
//        val input = packet.toByteArray()
//        val buffer = PacketBuffer()
//        buffer.put(input)
//
//        assertEquals(PacketBuffer.State.PACKET_COMPLETE, buffer.state)
//        assertEquals(packet, buffer.toPacket())
//        assertArrayEquals(byteArrayOf(), buffer.leftoverBytes)
//        assertThrows(IllegalStateException::class.java) { buffer.put(byteArrayOf(0, 4, 8)) }
//    }
//
//    @Test
//    fun incorrectUsernameTest() {
//        val timestamp = OffsetDateTime.now().withNano(0)
//        val fileSizeInfo = 256
//        val message = "duh"
//        val filename = "duh"
//        val validUsername = "A".repeat(0xFF)
//        val incorrectUsername = validUsername + "A"
//
//        assertDoesNotThrow { ConnectionRequest(validUsername) }
//        assertDoesNotThrow { ConnectionNotification(timestamp, validUsername) }
//        assertDoesNotThrow { DisconnectionNotification(timestamp, validUsername) }
//        assertDoesNotThrow { MessageNotification(timestamp, validUsername, message) }
//        assertDoesNotThrow { FileNotification(timestamp, fileSizeInfo, validUsername, filename) }
//
//        assertThrows(IllegalStateException::class.java) { ConnectionRequest(incorrectUsername) }
//        assertThrows(IllegalStateException::class.java) { ConnectionNotification(timestamp, incorrectUsername) }
//        assertThrows(IllegalStateException::class.java) { DisconnectionNotification(timestamp, incorrectUsername) }
//        assertThrows(IllegalStateException::class.java) { MessageNotification(timestamp, incorrectUsername, message) }
//        assertThrows(IllegalStateException::class.java) { FileNotification(timestamp, fileSizeInfo, incorrectUsername, message) }
//    }
//
//    @Test
//    fun incorrectMessageTest() {
//        val timestamp = OffsetDateTime.now().withNano(0)
//        val username = "ⓤⓢⓔⓡⓝⓐⓜⓔ"
//        val validMessage = "A".repeat(0xFFFF)
//        val incorrectMessage = validMessage + "A"
//
//        assertDoesNotThrow { MessageSent(validMessage) }
//        assertDoesNotThrow { MessageNotification(timestamp, username, validMessage) }
//
//        assertThrows(IllegalStateException::class.java) { MessageSent(incorrectMessage) }
//        assertThrows(IllegalStateException::class.java) { MessageNotification(timestamp, username, incorrectMessage) }
//    }
//
//    @Test
//    fun incorrectFilenameTest() {
//        val timestamp = OffsetDateTime.now().withNano(0)
//        val fileSizeInfo = 256
//        val username = "ⓤⓢⓔⓡⓝⓐⓜⓔ"
//        val validFilename = "A".repeat(0xFFFF)
//        val incorrectFilename = validFilename + "A"
//
//        assertDoesNotThrow { FileNotification(timestamp, fileSizeInfo, username, validFilename) }
//
//        assertThrows(IllegalStateException::class.java) { FileNotification(timestamp, fileSizeInfo, username, incorrectFilename) }
//    }
//
//    companion object {
//        @JvmStatic
//        fun fileUploadPacketSource(): Stream<Arguments> = Stream.of(
//            arguments(1, 256, "txt", true),
//            arguments(300000, 256, "txt", false),
//            arguments(1, Int.MAX_VALUE, "txt", false),
//            arguments(1, 256, "A".repeat(0x100), false)
//        )
//
//        @JvmStatic
//        fun fileUploadAcceptedPacketSource(): Stream<Arguments> = Stream.of(
//            arguments(1, 5000, true),
//            arguments(300000, 5000, false),
//            arguments(1, 70000, false)
//        )
//
//        @JvmStatic
//        fun fileUploadRejectedPacketSource(): Stream<Arguments> = Stream.of(
//            arguments(1, true),
//            arguments(300000, false)
//        )
//    }
//
//}

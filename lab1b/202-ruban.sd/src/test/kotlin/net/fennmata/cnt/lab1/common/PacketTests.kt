package net.fennmata.cnt.lab1.common

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer
import java.time.OffsetDateTime
import java.util.stream.Stream

class PacketTests {

    private fun commonTest(input: Packet, expected: ByteArray) {
        val actual = input.toByteArray()
        assertArrayEquals(expected, actual)
    }

    @ParameterizedTest
    @MethodSource("connectionRequestTestSource")
    fun connectionRequestTest(input: ConnectionRequest, expected: ByteArray) = commonTest(input, expected)

    @ParameterizedTest
    @MethodSource("connectionAcceptedTestSource")
    fun connectionAcceptedTest(input: ConnectionAccepted, expected: ByteArray) = commonTest(input, expected)

    @ParameterizedTest
    @MethodSource("connectionNotificationTestSource")
    fun connectionNotificationTest(input: ConnectionNotification, expected: ByteArray) = commonTest(input, expected)

    @ParameterizedTest
    @MethodSource("connectionRejectedTestSource")
    fun connectionRejectedTest(input: ConnectionRejected, expected: ByteArray) = commonTest(input, expected)

    @ParameterizedTest
    @MethodSource("disconnectionNotificationTestSource")
    fun disconnectionNotificationTest(input: DisconnectionNotification, expected: ByteArray) = commonTest(input, expected)

    @ParameterizedTest
    @MethodSource("messageSentTestSource")
    fun messageSentTest(input: MessageSent, expected: ByteArray) = commonTest(input, expected)

    @ParameterizedTest
    @MethodSource("messageNotificationTestSource")
    fun messageNotificationTest(input: MessageNotification, expected: ByteArray) = commonTest(input, expected)

    @ParameterizedTest
    @MethodSource("fileSentTestSource")
    fun fileSentTest(input: FileSent, expected: ByteArray) = commonTest(input, expected)

    @ParameterizedTest
    @MethodSource("fileNotificationPendingTestSource")
    fun fileNotificationPendingTest(input: FileNotificationPending, expected: ByteArray) = commonTest(input, expected)

    @ParameterizedTest
    @MethodSource("fileNotificationTestSource")
    fun fileNotificationTest(input: FileNotification, expected: ByteArray) = commonTest(input, expected)

    // TODO implement and test value validity checks per packet creation

    companion object {

        @JvmStatic
        fun connectionRequestTestSource(): Stream<Arguments> {
            val usernames = listOf("", "username", "ⓤⓢⓔⓡⓝⓐⓜⓔ")
            return usernames
                .map {
                    val bytes = it.encodeToByteArray()
                    arguments(
                        ConnectionRequest(it),
                        byteArrayOf(1, 0, bytes.size.toByte()) + bytes
                    )
                }
                .stream()
        }

        @JvmStatic
        fun connectionAcceptedTestSource(): Stream<Arguments> = Stream.of(
            arguments(ConnectionAccepted, byteArrayOf(1, 1))
        )

        @JvmStatic
        fun connectionNotificationTestSource(): Stream<Arguments> {
            val timestamp = OffsetDateTime.now().withNano(0)
            val username = "ⓤⓢⓔⓡⓝⓐⓜⓔ"
            val usernameBytes = username.encodeToByteArray()
            return Stream.of(
                arguments(
                    ConnectionNotification(timestamp, username),
                    byteArrayOf(1, 2) + timestamp.toByteArray() + byteArrayOf(usernameBytes.size.toByte()) + usernameBytes
                )
            )
        }

        @JvmStatic
        fun connectionRejectedTestSource(): Stream<Arguments> = Stream.of(
            arguments(ConnectionRejected, byteArrayOf(1, 3))
        )

        @JvmStatic
        fun disconnectionNotificationTestSource(): Stream<Arguments> {
            val timestamp = OffsetDateTime.now().withNano(0)
            val username = "ⓤⓢⓔⓡⓝⓐⓜⓔ"
            val usernameBytes = username.encodeToByteArray()
            return Stream.of(
                arguments(
                    DisconnectionNotification(timestamp, username),
                    byteArrayOf(1, 4) + timestamp.toByteArray() + byteArrayOf(usernameBytes.size.toByte()) + usernameBytes
                )
            )
        }

        @JvmStatic
        fun messageSentTestSource(): Stream<Arguments> {
            val messages = listOf("", "Hi there! How're you doing?", "ⓐ".repeat(10000))
            return messages
                .map {
                    val bytes = it.encodeToByteArray()
                    arguments(
                        MessageSent(it),
                        byteArrayOf(1, 5) + bytes.size.toByteArray(2) + bytes
                    )
                }
                .stream()
        }

        @JvmStatic
        fun messageNotificationTestSource(): Stream<Arguments> {
            val timestamp = OffsetDateTime.now().withNano(0)
            val username = "ⓤⓢⓔⓡⓝⓐⓜⓔ"
            val message = "ⓐ".repeat(10000)
            val usernameBytes = username.encodeToByteArray()
            val messageBytes = message.encodeToByteArray()
            val packetBuffer = ByteBuffer.allocate(
                13 + usernameBytes.size + messageBytes.size
            ).apply {
                put(1)
                put(6)
                put(timestamp.toByteArray())
                put(usernameBytes.size.toByteArray(1))
                put(messageBytes.size.toByteArray(2))
                put(usernameBytes)
                put(messageBytes)
            }
            return Stream.of(
                arguments(
                    MessageNotification(timestamp, username, message),
                    packetBuffer.array()
                )
            )
        }

        @JvmStatic
        fun fileSentTestSource(): Stream<Arguments> {
            val contents = listOf(
                "" to "".encodeToByteArray().toList(),
                "text.txt" to "this is a file text".encodeToByteArray().toList(),
                "ⓐ.txt" to "ⓐ".repeat(10000).encodeToByteArray().toList()
            )
            return contents
                .map { (filename, file) ->
                    val filenameBytes = filename.encodeToByteArray()
                    val fileBytes = file.toByteArray()
                    val packetBuffer = ByteBuffer.allocate(
                        6 + filenameBytes.size + fileBytes.size
                    ).apply {
                        put(1)
                        put(7)
                        put(filenameBytes.size.toByteArray(1))
                        put(fileBytes.size.toByteArray(3))
                        put(filenameBytes)
                        put(fileBytes)
                    }
                    arguments(
                        FileSent(filename, file),
                        packetBuffer.array()
                    )
                }
                .stream()
        }

        @JvmStatic
        fun fileNotificationPendingTestSource(): Stream<Arguments> {
            val expiryTime = OffsetDateTime.now().withNano(0)
            val socketPorts = listOf(1024, 2354, 6456, 32453)
            return socketPorts
                .map {
                    arguments(
                        FileNotificationPending(it, expiryTime),
                        byteArrayOf(1, 8) + it.toByteArray(2) + expiryTime.toByteArray()
                    )
                }.stream()
        }

        @JvmStatic
        fun fileNotificationTestSource(): Stream<Arguments> {
            val timestamp = OffsetDateTime.now().withNano(0)
            val username = "ⓤⓢⓔⓡⓝⓐⓜⓔ"
            val filename = "ⓐ.txt"
            val file = "ⓐ".repeat(10000).encodeToByteArray().toList()
            val usernameBytes = username.encodeToByteArray()
            val filenameBytes = filename.encodeToByteArray()
            val fileBytes = file.toByteArray()
            val packetBuffer = ByteBuffer.allocate(
                15 + usernameBytes.size + filenameBytes.size + fileBytes.size
            ).apply {
                put(1)
                put(9)
                put(timestamp.toByteArray())
                put(usernameBytes.size.toByteArray(1))
                put(filenameBytes.size.toByteArray(1))
                put(fileBytes.size.toByteArray(3))
                put(usernameBytes)
                put(filenameBytes)
                put(fileBytes)
            }
            return Stream.of(
                arguments(
                    FileNotification(timestamp, username, filename, file),
                    packetBuffer.array()
                )
            )
        }

    }

}

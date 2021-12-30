package net.fennmata.cnt.lab1.common

import java.nio.ByteBuffer
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

sealed class Packet(protected val event: PacketEvent) {
    fun toByteArray() = header + body

    private val header by lazy { byteArrayOf(1, event.number) }
    protected abstract val body: ByteArray
}

data class ConnectionRequest(
    val username: String
) : Packet(ConnectionRequestEvent) {
    private val usernameBytes = username.encodeToByteArray()
    private val usernameLength = usernameBytes.size

    init {
        check(usernameLength <= Username.maxWidth) { "Username is too big for transmission" }
    }

    override val body: ByteArray by lazy {
        val buffer = ByteBuffer.allocate(event.bodyConstsWidth + usernameLength)
        buffer.apply {
            put(usernameLength.toByteArray(UsernameLength.width))
            put(usernameBytes)
        }
        buffer.array()
    }
}

object ConnectionAccepted : Packet(ConnectionAcceptedEvent) {
    override val body: ByteArray = byteArrayOf()
}

data class ConnectionNotification(
    val timestamp: OffsetDateTime,
    val username: String
) : Packet(ConnectionNotificationEvent) {
    private val usernameBytes = username.encodeToByteArray()
    private val usernameLength = usernameBytes.size

    init {
        check(usernameLength <= Username.maxWidth) { "Username is too big for transmission" }
    }

    override val body: ByteArray by lazy {
        val buffer = ByteBuffer.allocate(event.bodyConstsWidth + usernameLength)
        buffer.apply {
            putLong(timestamp.toEpochSecond())
            put(usernameLength.toByteArray(UsernameLength.width))
            put(usernameBytes)
        }
        buffer.array()
    }
}

object ConnectionRejected : Packet(ConnectionRejectedEvent) {
    override val body: ByteArray = byteArrayOf()
}

data class DisconnectionNotification(
    val timestamp: OffsetDateTime,
    val username: String
) : Packet(DisconnectionNotificationEvent) {
    private val usernameBytes = username.encodeToByteArray()
    private val usernameLength = usernameBytes.size

    init {
        check(usernameLength <= Username.maxWidth) { "Username is too big for transmission" }
    }

    override val body: ByteArray by lazy {
        val buffer = ByteBuffer.allocate(event.bodyConstsWidth + usernameLength)
        buffer.apply {
            putLong(timestamp.toEpochSecond())
            put(usernameLength.toByteArray(UsernameLength.width))
            put(usernameBytes)
        }
        buffer.array()
    }
}

data class MessageSent(
    val message: String
) : Packet(MessageSentEvent) {
    private val messageBytes = message.encodeToByteArray()
    private val messageLength = messageBytes.size

    init {
        check(messageLength <= Message.maxWidth) { "Message is too big for transmission" }
    }

    override val body: ByteArray by lazy {
        val buffer = ByteBuffer.allocate(event.bodyConstsWidth + messageLength)
        buffer.apply {
            put(messageLength.toByteArray(MessageLength.width))
            put(messageBytes)
        }
        buffer.array()
    }
}

data class MessageNotification(
    val timestamp: OffsetDateTime,
    val username: String,
    val message: String
) : Packet(MessageNotificationEvent) {
    private val usernameBytes = username.encodeToByteArray()
    private val usernameLength = usernameBytes.size
    private val messageBytes = message.encodeToByteArray()
    private val messageLength = messageBytes.size

    init {
        check(usernameLength <= Username.maxWidth) { "Username is too big for transmission" }
        check(messageLength <= Message.maxWidth) { "Message is too big for transmission" }
    }

    override val body: ByteArray by lazy {
        val buffer = ByteBuffer.allocate(event.bodyConstsWidth + usernameLength + messageLength)
        buffer.apply {
            putLong(timestamp.toEpochSecond())
            put(usernameLength.toByteArray(UsernameLength.width))
            put(messageLength.toByteArray(MessageLength.width))
            put(usernameBytes)
            put(messageBytes)
        }
        buffer.array()
    }
}

// TODO define all other packets

fun PacketEvent.getAdditionalBodyVarsWidth(packetInfo: Map<Any, Int>): Int = when (this) {
    ConnectionRequestEvent -> 0
    ConnectionAcceptedEvent -> 0
    ConnectionNotificationEvent -> 0
    ConnectionRejectedEvent -> 0
    DisconnectionNotificationEvent -> 0
    MessageSentEvent -> 0
    MessageNotificationEvent -> 0
    FileUploadEvent -> 0
    FileUploadAcceptedEvent -> 0
    FileUploadedEvent -> {
        packetInfo[FileSize]
            ?: throw IllegalStateException("parsing of FileUploaded packet needs fileSize to be passed to PacketBuffer")
    }
    FileNotificationEvent -> 0
    FileUploadRejectedEvent -> 0
    FileDownloadEvent -> 0
    FileDownloadAcceptedEvent -> 0
    FileDownloadedEvent -> 0
    FileDownloadRejectedEvent -> 0
}

fun buildPacket(event: PacketEvent, body: ByteArray): Packet = when (event) {
    ConnectionRequestEvent -> {
        val usernameLength = body.sliceArray(event.getRangeOf(UsernameLength)).toIntWithoutSign()
        val usernameBytes = body.sliceArray(event.bodyConstsWidth until event.bodyConstsWidth + usernameLength)
        val username = usernameBytes.decodeToString()
        ConnectionRequest(username)
    }
    ConnectionAcceptedEvent -> ConnectionAccepted
    ConnectionNotificationEvent -> {
        val timestampSeconds = body.sliceArray(event.getRangeOf(Timestamp)).toLongWithoutSign()
        val timestampInstant = Instant.ofEpochSecond(timestampSeconds)
        val timestamp = OffsetDateTime.ofInstant(timestampInstant, ZoneId.systemDefault())
        val usernameLength = body.sliceArray(event.getRangeOf(UsernameLength)).toIntWithoutSign()
        val usernameBytes = body.sliceArray(event.bodyConstsWidth until event.bodyConstsWidth + usernameLength)
        val username = usernameBytes.decodeToString()
        ConnectionNotification(timestamp, username)
    }
    ConnectionRejectedEvent -> ConnectionRejected
    DisconnectionNotificationEvent -> {
        val timestampSeconds = body.sliceArray(event.getRangeOf(Timestamp)).toLongWithoutSign()
        val timestampInstant = Instant.ofEpochSecond(timestampSeconds)
        val timestamp = OffsetDateTime.ofInstant(timestampInstant, ZoneId.systemDefault())
        val usernameLength = body.sliceArray(event.getRangeOf(UsernameLength)).toIntWithoutSign()
        val usernameBytes = body.sliceArray(event.bodyConstsWidth until event.bodyConstsWidth + usernameLength)
        val username = usernameBytes.decodeToString()
        DisconnectionNotification(timestamp, username)
    }
    MessageSentEvent -> {
        val messageLength = body.sliceArray(event.getRangeOf(MessageLength)).toIntWithoutSign()
        val messageBytes = body.sliceArray(event.bodyConstsWidth until event.bodyConstsWidth + messageLength)
        val message = messageBytes.decodeToString()
        MessageSent(message)
    }
    MessageNotificationEvent -> {
        val timestampSeconds = body.sliceArray(event.getRangeOf(Timestamp)).toLongWithoutSign()
        val timestampInstant = Instant.ofEpochSecond(timestampSeconds)
        val timestamp = OffsetDateTime.ofInstant(timestampInstant, ZoneId.systemDefault())
        val usernameLength = body.sliceArray(event.getRangeOf(UsernameLength)).toIntWithoutSign()
        val usernameStart = event.bodyConstsWidth
        val usernameBytes = body.sliceArray(usernameStart until usernameStart + usernameLength)
        val username = usernameBytes.decodeToString()
        val messageLength = body.sliceArray(event.getRangeOf(MessageLength)).toIntWithoutSign()
        val messageStart = usernameStart + usernameLength
        val messageBytes = body.sliceArray(messageStart until messageStart + messageLength)
        val message = messageBytes.decodeToString()
        MessageNotification(timestamp, username, message)
    }
    FileUploadEvent -> TODO()
    FileUploadAcceptedEvent -> TODO()
    FileUploadedEvent -> TODO()
    FileNotificationEvent -> TODO()
    FileUploadRejectedEvent -> TODO()
    FileDownloadEvent -> TODO()
    FileDownloadAcceptedEvent -> TODO()
    FileDownloadedEvent -> TODO()
    FileDownloadRejectedEvent -> TODO()
}

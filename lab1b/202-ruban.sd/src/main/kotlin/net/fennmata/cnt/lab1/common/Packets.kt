package net.fennmata.cnt.lab1.common

import java.time.OffsetDateTime

interface Packet {
    val event: PacketEvent
}

interface PacketWithTimestamp : Packet {
    val timestamp: OffsetDateTime
}
interface PacketWithSocketPort : Packet {
    val socketPort: Int
}
interface PacketWithExpiryTime : Packet {
    val expiryTime: OffsetDateTime
}
interface PacketWithUsername : Packet {
    val username: String
    val usernameBytes: ByteArray get() = username.encodeToByteArray()
}
interface PacketWithMessage : Packet {
    val message: String
    val messageBytes: ByteArray get() = message.encodeToByteArray()
}
interface PacketWithFilename : Packet {
    val filename: String
    val filenameBytes: ByteArray get() = filename.encodeToByteArray()
}
interface PacketWithFile : Packet {
    val file: List<Byte>
}

interface CompanionWithTimestamp : PacketWithTimestamp {
    override val timestamp get() = reportNonexistentField()
}
interface CompanionWithSocketPort : PacketWithSocketPort {
    override val socketPort get() = reportNonexistentField()
}
interface CompanionWithExpiryTime : PacketWithExpiryTime {
    override val expiryTime get() = reportNonexistentField()
}
interface CompanionWithUsername : PacketWithUsername {
    override val username get() = reportNonexistentField()
}
interface CompanionWithMessage : PacketWithMessage {
    override val message get() = reportNonexistentField()
}
interface CompanionWithFilename : PacketWithFilename {
    override val filename get() = reportNonexistentField()
}
interface CompanionWithFile : PacketWithFile {
    override val file get() = reportNonexistentField()
}

private fun reportNonexistentField(): Nothing =
    error("Companion objects of Packet implementations don't have accessible Packet fields")

data class ConnectionRequest(
    override val username: String
) : PacketWithUsername {
    override val event get() = Companion.event
    companion object : CompanionWithUsername {
        override val event = PacketEvent.CONNECTION_REQUEST
    }
}

object ConnectionAccepted : Packet {
    override val event = PacketEvent.CONNECTION_ACCEPTED
}

data class ConnectionNotification(
    override val timestamp: OffsetDateTime,
    override val username: String
) : PacketWithTimestamp, PacketWithUsername {
    override val event get() = Companion.event
    companion object : CompanionWithTimestamp, CompanionWithUsername {
        override val event = PacketEvent.CONNECTION_NOTIFICATION
    }
}

object ConnectionRejected : Packet {
    override val event = PacketEvent.CONNECTION_REJECTED
}

data class DisconnectionNotification(
    override val timestamp: OffsetDateTime,
    override val username: String
) : PacketWithTimestamp, PacketWithUsername {
    override val event get() = Companion.event
    companion object : CompanionWithTimestamp, CompanionWithUsername {
        override val event = PacketEvent.DISCONNECTION_NOTIFICATION
    }
}

data class MessageSent(
    override val message: String
) : PacketWithMessage {
    override val event get() = Companion.event
    companion object : CompanionWithMessage {
        override val event = PacketEvent.MESSAGE_SENT
    }
}

data class MessageNotification(
    override val timestamp: OffsetDateTime,
    override val username: String,
    override val message: String
) : PacketWithTimestamp, PacketWithUsername, PacketWithMessage {
    override val event get() = Companion.event
    companion object : CompanionWithTimestamp, CompanionWithUsername, CompanionWithMessage {
        override val event = PacketEvent.MESSAGE_NOTIFICATION
    }
}

data class FileSent(
    override val filename: String,
    override val file: List<Byte>
) : PacketWithFilename, PacketWithFile {
    override val event get() = Companion.event
    companion object : CompanionWithFilename, CompanionWithFile {
        override val event = PacketEvent.FILE_SENT
    }
}

data class FileNotificationPending(
    override val socketPort: Int,
    override val expiryTime: OffsetDateTime
) : PacketWithSocketPort, PacketWithExpiryTime {
    override val event get() = Companion.event
    companion object : CompanionWithSocketPort, CompanionWithExpiryTime {
        override val event = PacketEvent.FILE_NOTIFICATION_PENDING
    }
}

data class FileNotification(
    override val timestamp: OffsetDateTime,
    override val username: String,
    override val filename: String,
    override val file: List<Byte>
) : PacketWithTimestamp, PacketWithUsername, PacketWithFilename, PacketWithFile {
    override val event get() = Companion.event
    companion object : CompanionWithTimestamp, CompanionWithUsername, CompanionWithFilename, CompanionWithFile {
        override val event = PacketEvent.FILE_NOTIFICATION
    }
}

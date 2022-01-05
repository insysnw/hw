package net.fennmata.cnt.lab1.common

import java.nio.ByteBuffer

fun Packet.toByteArray(): ByteArray {
    val header = byteArrayOf(1, event.toByte())

    val constantBodyPartBuffer = ByteBuffer.allocate(event.constantBodyPartWidth)

    var variableBodyPartWidth = 0
    if (this is PacketWithUsername) variableBodyPartWidth += usernameBytes.size
    if (this is PacketWithMessage) variableBodyPartWidth += messageBytes.size
    if (this is PacketWithFilename) variableBodyPartWidth += filenameBytes.size
    if (this is PacketWithFile) variableBodyPartWidth += file.size
    val variableBodyPartBuffer = ByteBuffer.allocate(variableBodyPartWidth)

    fun writeFieldValue(field: VariableWidthPacketField, value: ByteArray) {
        constantBodyPartBuffer.put(value.size.toByteArray(field.widthField.width))
        variableBodyPartBuffer.put(value)
    }

    if (this is PacketWithTimestamp) {
        constantBodyPartBuffer.put(timestamp.toByteArray())
    }
    if (this is PacketWithSocketPort) {
        constantBodyPartBuffer.put(socketPort.toByteArray(ConstantWidthPacketField.SOCKET_PORT.width))
    }
    if (this is PacketWithExpiryTime) {
        constantBodyPartBuffer.put(expiryTime.toByteArray())
    }
    if (this is PacketWithUsername) writeFieldValue(VariableWidthPacketField.USERNAME, usernameBytes)
    if (this is PacketWithMessage) writeFieldValue(VariableWidthPacketField.MESSAGE, messageBytes)
    if (this is PacketWithFilename) writeFieldValue(VariableWidthPacketField.FILENAME, filenameBytes)
    if (this is PacketWithFile) writeFieldValue(VariableWidthPacketField.FILE, file.toByteArray())

    return header + constantBodyPartBuffer.array() + variableBodyPartBuffer.array()
}

fun PacketEvent.compilePacket(constantBodyPart: ByteArray, variableBodyPart: ByteArray): Packet {
    fun PacketEvent.readFieldValue(field: ConstantWidthPacketField): ByteArray {
        val indices = indicesOfConstantWidthFields[field] ?: error("A packet in question does not have a $field field")
        return constantBodyPart.sliceArray(indices)
    }

    val indicesOfVariableWidthFields = mutableMapOf<VariableWidthPacketField, IntRange>()
    var fieldStart = 0

    fun registerFieldIndices(field: VariableWidthPacketField) {
        val widthIndices = indicesOfConstantWidthFields[field.widthField]
            ?: error("A packet in question does not have a $field field")
        val width = constantBodyPart.sliceArray(widthIndices).toUnsignedInt()
        indicesOfVariableWidthFields[field] = fieldStart until fieldStart + width
        fieldStart += width
    }

    val structureReference = structureReference
    if (structureReference is PacketWithUsername) registerFieldIndices(VariableWidthPacketField.USERNAME)
    if (structureReference is PacketWithMessage) registerFieldIndices(VariableWidthPacketField.MESSAGE)
    if (structureReference is PacketWithFilename) registerFieldIndices(VariableWidthPacketField.FILENAME)
    if (structureReference is PacketWithFile) registerFieldIndices(VariableWidthPacketField.FILE)

    fun readFieldValue(field: VariableWidthPacketField): ByteArray {
        val indices = indicesOfVariableWidthFields[field] ?: error("A packet in question does not have a $field field")
        return variableBodyPart.sliceArray(indices)
    }

    return when (this) {
        PacketEvent.CONNECTION_REQUEST -> {
            val usernameBytes = readFieldValue(VariableWidthPacketField.USERNAME)
            ConnectionRequest(usernameBytes.decodeToString())
        }
        PacketEvent.CONNECTION_ACCEPTED -> {
            ConnectionAccepted
        }
        PacketEvent.CONNECTION_NOTIFICATION -> {
            val timestampBytes = readFieldValue(ConstantWidthPacketField.TIMESTAMP)
            val usernameBytes = readFieldValue(VariableWidthPacketField.USERNAME)
            ConnectionNotification(timestampBytes.toOffsetDateTime(), usernameBytes.decodeToString())
        }
        PacketEvent.CONNECTION_REJECTED -> {
            ConnectionRejected
        }
        PacketEvent.DISCONNECTION_NOTIFICATION -> {
            val timestampBytes = readFieldValue(ConstantWidthPacketField.TIMESTAMP)
            val usernameBytes = readFieldValue(VariableWidthPacketField.USERNAME)
            DisconnectionNotification(timestampBytes.toOffsetDateTime(), usernameBytes.decodeToString())
        }
        PacketEvent.MESSAGE_SENT -> {
            val messageBytes = readFieldValue(VariableWidthPacketField.MESSAGE)
            MessageSent(messageBytes.decodeToString())
        }
        PacketEvent.MESSAGE_NOTIFICATION -> {
            val timestampBytes = readFieldValue(ConstantWidthPacketField.TIMESTAMP)
            val usernameBytes = readFieldValue(VariableWidthPacketField.USERNAME)
            val messageBytes = readFieldValue(VariableWidthPacketField.MESSAGE)
            MessageNotification(
                timestampBytes.toOffsetDateTime(),
                usernameBytes.decodeToString(),
                messageBytes.decodeToString()
            )
        }
        PacketEvent.FILE_SENT -> {
            val filenameBytes = readFieldValue(VariableWidthPacketField.FILENAME)
            val file = readFieldValue(VariableWidthPacketField.FILE)
            FileSent(filenameBytes.decodeToString(), file.toList())
        }
        PacketEvent.FILE_NOTIFICATION_PENDING -> {
            val socketPortBytes = readFieldValue(ConstantWidthPacketField.SOCKET_PORT)
            val expiryTimeBytes = readFieldValue(ConstantWidthPacketField.EXPIRY_TIME)
            FileNotificationPending(socketPortBytes.toUnsignedInt(), expiryTimeBytes.toOffsetDateTime())
        }
        PacketEvent.FILE_NOTIFICATION -> {
            val timestampBytes = readFieldValue(ConstantWidthPacketField.TIMESTAMP)
            val usernameBytes = readFieldValue(VariableWidthPacketField.USERNAME)
            val filenameBytes = readFieldValue(VariableWidthPacketField.FILENAME)
            val file = readFieldValue(VariableWidthPacketField.FILE)
            FileNotification(
                timestampBytes.toOffsetDateTime(),
                usernameBytes.decodeToString(),
                filenameBytes.decodeToString(),
                file.toList()
            )
        }
    }
}

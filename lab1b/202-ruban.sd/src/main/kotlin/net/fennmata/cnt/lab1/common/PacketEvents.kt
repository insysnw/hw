package net.fennmata.cnt.lab1.common

enum class PacketEvent {
    CONNECTION_REQUEST,
    CONNECTION_ACCEPTED,
    CONNECTION_NOTIFICATION,
    CONNECTION_REJECTED,
    DISCONNECTION_NOTIFICATION,
    MESSAGE_SENT,
    MESSAGE_NOTIFICATION,
    FILE_SENT,
    FILE_NOTIFICATION_PENDING,
    FILE_NOTIFICATION;

    fun toByte(): Byte = ordinal.toByte()

    val indicesOfConstantWidthFields: Map<ConstantWidthPacketField, IntRange> by lazy {
        val result = mutableMapOf<ConstantWidthPacketField, IntRange>()
        var fieldStart = 0

        fun registerFieldIndices(field: ConstantWidthPacketField) {
            result[field] = fieldStart until fieldStart + field.width
            fieldStart += field.width
        }

        val structureReference = structureReference
        if (structureReference is PacketWithTimestamp) registerFieldIndices(ConstantWidthPacketField.TIMESTAMP)
        if (structureReference is PacketWithSocketPort) registerFieldIndices(ConstantWidthPacketField.SOCKET_PORT)
        if (structureReference is PacketWithExpiryTime) registerFieldIndices(ConstantWidthPacketField.EXPIRY_TIME)
        if (structureReference is PacketWithUsername) registerFieldIndices(VariableWidthPacketField.USERNAME.widthField)
        if (structureReference is PacketWithMessage) registerFieldIndices(VariableWidthPacketField.MESSAGE.widthField)
        if (structureReference is PacketWithFilename) registerFieldIndices(VariableWidthPacketField.FILENAME.widthField)
        if (structureReference is PacketWithFile) registerFieldIndices(VariableWidthPacketField.FILE.widthField)

        result
    }

    val constantBodyPartWidth: Int by lazy {
        indicesOfConstantWidthFields.values
            .fold(emptyList<Int>()) { acc, next -> acc + next }
            .lastOrNull()
            ?.let { it + 1 }
            ?: 0
    }

    fun calculateVariableBodyPartWidth(constantBodyPart: ByteArray): Int {
        var result = 0

        fun getWidthOf(field: VariableWidthPacketField): Int {
            val widthFieldIndices = indicesOfConstantWidthFields[field.widthField]
                ?: error("A packet in question does not have a $field field")
            val widthFieldBytes = constantBodyPart.sliceArray(widthFieldIndices)
            return widthFieldBytes.toUnsignedInt()
        }

        val structureReference = structureReference
        if (structureReference is PacketWithUsername) result += getWidthOf(VariableWidthPacketField.USERNAME)
        if (structureReference is PacketWithMessage) result += getWidthOf(VariableWidthPacketField.MESSAGE)
        if (structureReference is PacketWithFilename) result += getWidthOf(VariableWidthPacketField.FILENAME)
        if (structureReference is PacketWithFile) result += getWidthOf(VariableWidthPacketField.FILE)

        return result
    }

}

val PacketEvent.structureReference: Packet get() = when (this) {
    PacketEvent.CONNECTION_REQUEST -> ConnectionRequest
    PacketEvent.CONNECTION_ACCEPTED -> ConnectionAccepted
    PacketEvent.CONNECTION_NOTIFICATION -> ConnectionNotification
    PacketEvent.CONNECTION_REJECTED -> ConnectionRejected
    PacketEvent.DISCONNECTION_NOTIFICATION -> DisconnectionNotification
    PacketEvent.MESSAGE_SENT -> MessageSent
    PacketEvent.MESSAGE_NOTIFICATION -> MessageNotification
    PacketEvent.FILE_SENT -> FileSent
    PacketEvent.FILE_NOTIFICATION_PENDING -> FileNotificationPending
    PacketEvent.FILE_NOTIFICATION -> FileNotification
}

fun Byte.toPacketEvent(): PacketEvent? = PacketEvent.values().find { it.ordinal.toByte() == this }

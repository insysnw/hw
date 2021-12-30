package net.fennmata.cnt.lab1.common

sealed class PacketEvent(val number: Byte, vararg val fields: PacketField) {
    init {
        var wasVariableWidthFieldFound = false
        for (field in fields) {
            when (field) {
                is ConstantWidthField -> {
                    check(!wasVariableWidthFieldFound) { "Field order for PacketEvent #$number is wrong" }
                }
                is VariableWidthField -> {
                    wasVariableWidthFieldFound = true
                }
            }
        }
    }
}

object ConnectionRequestEvent : PacketEvent(0, UsernameLength, Username)
object ConnectionAcceptedEvent : PacketEvent(1)
object ConnectionNotificationEvent : PacketEvent(2, Timestamp, UsernameLength, Username)
object ConnectionRejectedEvent : PacketEvent(3)
object DisconnectionNotificationEvent : PacketEvent(4, Timestamp, UsernameLength, Username)
object MessageSentEvent : PacketEvent(5, MessageLength, Message)
object MessageNotificationEvent : PacketEvent(6, Timestamp, UsernameLength, MessageLength, Username, Message)
object FileUploadEvent : PacketEvent(7, FileId, FileSize, FileExtensionLength, FileExtension)
object FileUploadAcceptedEvent : PacketEvent(8, FileId, SocketPort)
object FileUploadedEvent : PacketEvent(9, File)
object FileNotificationEvent : PacketEvent(10, Timestamp, FileSizeInfo, UsernameLength, FilenameLength, Username, Filename)
object FileUploadRejectedEvent : PacketEvent(11, FileId)
object FileDownloadEvent : PacketEvent(12, FilenameLength, Filename)
object FileDownloadAcceptedEvent : PacketEvent(13, SocketPort, FilenameLength, Filename)
object FileDownloadedEvent : PacketEvent(14, FileSize, File)
object FileDownloadRejectedEvent : PacketEvent(15, FilenameLength, Filename)

fun getPacketEventByNumber(number: Int): PacketEvent? = when (number) {
    0 -> ConnectionRequestEvent
    1 -> ConnectionAcceptedEvent
    2 -> ConnectionNotificationEvent
    3 -> ConnectionRejectedEvent
    4 -> DisconnectionNotificationEvent
    5 -> MessageSentEvent
    6 -> MessageNotificationEvent
    7 -> FileUploadEvent
    8 -> FileUploadAcceptedEvent
    9 -> FileUploadedEvent
    10 -> FileNotificationEvent
    11 -> FileUploadRejectedEvent
    12 -> FileDownloadEvent
    13 -> FileDownloadAcceptedEvent
    14 -> FileDownloadedEvent
    15 -> FileDownloadRejectedEvent
    else -> null
}

val PacketEvent.bodyConstsWidth: Int get() = fields.filterIsInstance<ConstantWidthField>().sumOf { it.width }

fun PacketEvent.getRangeOf(targetField: ConstantWidthField): IntRange {
    var rangeStart = 0
    for (field in fields) {
        if (field == targetField) return rangeStart until rangeStart + targetField.width
        if (field is ConstantWidthField)
            rangeStart += field.width
        else
            break
    }
    throw IllegalArgumentException("$targetField isn't in the field order for PacketEvent #$number")
}

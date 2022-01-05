package net.fennmata.cnt.lab1.common

enum class ConstantWidthPacketField(val width: Int) {
    TIMESTAMP(8),
    SOCKET_PORT(2),
    EXPIRY_TIME(8),
    USERNAME_LENGTH(1),
    MESSAGE_LENGTH(2),
    FILENAME_LENGTH(1),
    FILE_SIZE(3)
}

enum class VariableWidthPacketField(val widthField: ConstantWidthPacketField) {
    USERNAME(ConstantWidthPacketField.USERNAME_LENGTH),
    MESSAGE(ConstantWidthPacketField.MESSAGE_LENGTH),
    FILENAME(ConstantWidthPacketField.FILENAME_LENGTH),
    FILE(ConstantWidthPacketField.FILE_SIZE)
}

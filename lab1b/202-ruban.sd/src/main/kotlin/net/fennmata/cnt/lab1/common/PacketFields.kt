package net.fennmata.cnt.lab1.common

sealed interface PacketField

sealed class ConstantWidthField(val width: Int) : PacketField
object Timestamp : ConstantWidthField(8)
object FileId : ConstantWidthField(2)
object SocketPort : ConstantWidthField(2)
object FileSizeInfo : ConstantWidthField(3)

sealed class WidthStorageField(width: Int) : ConstantWidthField(width)
object UsernameLength : WidthStorageField(1)
object MessageLength : WidthStorageField(2)
object FileSize : WidthStorageField(3)
object FileExtensionLength : WidthStorageField(1)
object FilenameLength : WidthStorageField(2)

sealed class VariableWidthField(val widthStorage: WidthStorageField) : PacketField
object Username : VariableWidthField(UsernameLength)
object Message : VariableWidthField(MessageLength)
object File : VariableWidthField(FileSize)
object FileExtension : VariableWidthField(FileExtensionLength)
object Filename : VariableWidthField(FilenameLength)

val ConstantWidthField.maxValue: Int get() = (-1 shl 8 * width).inv()

val VariableWidthField.maxWidth: Int get() = (1 shl 8 * widthStorage.width) - 1

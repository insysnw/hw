package net.fennmata.cnt.lab2.tftp

fun Short.toByteArray(): ByteArray = byteArrayOf((toInt() ushr 8).toByte(), toByte())
fun ByteArray.nullTerminated(): ByteArray = this + byteArrayOf(0)
fun String.toASCII(): ByteArray = toByteArray(Charsets.US_ASCII)
fun String.toNullTerminatedASCII(): ByteArray = toASCII().nullTerminated()

enum class RequestType { WRITE, READ }
fun RequestType.toByteArray() = byteArrayOf(0, (ordinal + 1).toByte())

enum class TransmissionMode { NETASCII, OCTET }
fun TransmissionMode.toByteArray() = name.lowercase().toNullTerminatedASCII()

enum class ErrorCode {
    NOT_DEFINED,
    FILE_NOT_FOUND,
    ACCESS_VIOLATION,
    NO_MEMORY_SPACE,
    ILLEGAL_OPERATION,
    UNKNOWN_TRANSFER_ID,
    FILE_ALREADY_EXISTS,
    NO_SUCH_USER
}
fun ErrorCode.toByteArray() = byteArrayOf(0, ordinal.toByte())

fun ByteArray.toTFTPPacket(): TFTPPacket {
    when (get(1).toInt()) {
        1, 2 -> TODO()
        3 -> TODO()
        4 -> TODO()
        5 -> TODO()
        else -> throw IllegalStateException("Illegal packet received")
    }
}

package net.fennmata.cnt.lab2.tftp

fun Short.toByteArray(): ByteArray = byteArrayOf((toInt() ushr 8).toByte(), toByte())
fun ByteArray.nullTerminated(): ByteArray = this + byteArrayOf(0)
fun String.toASCII(): ByteArray = toByteArray(Charsets.US_ASCII)
fun String.toNullTerminatedASCII(): ByteArray = toASCII().nullTerminated()

enum class RequestType { READ, WRITE }
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

fun List<Byte>.toUnsignedNumber(): Long {
    return map { it.toLong() and 0xFF }.fold(0L) { acc, next -> (acc shl 8) or next }
}

fun List<Byte>.toUnsignedInt(): Int = toUnsignedNumber().toInt()
fun List<Byte>.toUnsignedShort(): Short = toUnsignedNumber().toShort()

fun ByteArray.toUnsignedInt(): Int = toList().toUnsignedInt()
fun ByteArray.toUnsignedShort(): Short = toList().toUnsignedShort()

fun ByteArray.toTFTPPacket(): TFTPPacket {
    fun toRequestPacket(type: RequestType, body: ByteArray): Request {
        val filename: String
        val mode: TransmissionMode
        body.split(0).also { bytes ->
            filename = bytes[0].toString(Charsets.US_ASCII)
            val modeString = bytes[1].toString(Charsets.US_ASCII)
            mode = TransmissionMode.values().find { it.name == modeString.uppercase() }
                ?: throw IllegalStateException("packet with unknown transmission mode received")
        }
        return Request(type, filename, mode)
    }

    val body = sliceArray(2 until size)
    return when (get(1).toInt()) {
        1 -> toRequestPacket(RequestType.READ, body)
        2 -> toRequestPacket(RequestType.WRITE, body)
        3 -> DataBlock(
            sliceArray(2 until 4).toUnsignedShort(),
            sliceArray(4 until size)
        )
        4 -> Acknowledgement(sliceArray(2 until 4).toUnsignedShort())
        5 -> {
            val code = ErrorCode.values().find { it.ordinal == sliceArray(2 until 4).toUnsignedInt() }
                ?: throw IllegalStateException("packet with unknown error code received")
            Error(
                code,
                sliceArray(4 until size - 1).toString(Charsets.US_ASCII)
            )
        }
        else -> throw IllegalStateException("Illegal packet received")
    }
}

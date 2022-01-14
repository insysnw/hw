package net.fennmata.cnt.lab2.tftp

sealed interface TFTPPacket {
    fun toByteArray(): ByteArray
}

data class Request(val type: RequestType, val filename: String, val mode: TransmissionMode) : TFTPPacket {
    override fun toByteArray(): ByteArray {
        return type.toByteArray() + filename.toNullTerminatedASCII() + mode.toByteArray()
    }
}

data class DataBlock(val number: Short, val data: ByteArray) : TFTPPacket {
    override fun toByteArray(): ByteArray {
        return byteArrayOf(0, 3) + number.toByteArray() + data
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataBlock

        if (number != other.number) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = number.toInt()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

data class Acknowledgement(val number: Short) : TFTPPacket {
    override fun toByteArray(): ByteArray {
        return byteArrayOf(0, 4) + number.toByteArray()
    }
}

data class Error(val code: ErrorCode, val message: String) : TFTPPacket {
    override fun toByteArray(): ByteArray {
        return byteArrayOf(0, 5) + code.toByteArray() + message.toNullTerminatedASCII()
    }
}

package net.fennmata.cnt.lab1.common

import java.nio.ByteBuffer

fun List<Byte>.toNumberWithoutSign(): Long {
    return map { it.toLong() and 0xFF }.fold(0L) { accumulator, next -> (accumulator shl 8) or next }
}

fun List<Byte>.toLongWithoutSign(): Long = toNumberWithoutSign()
fun List<Byte>.toIntWithoutSign(): Int = toNumberWithoutSign().toInt()

fun ByteArray.toLongWithoutSign(): Long = toList().toLongWithoutSign()
fun ByteArray.toIntWithoutSign(): Int = toList().toIntWithoutSign()

fun Long.toByteArray(capacity: Int): ByteArray {
    return ByteBuffer.allocate(8)
        .also { it.putLong(this) }.array()
        .let { it.sliceArray(it.size - capacity until it.size) }
}
fun Int.toByteArray(capacity: Int): ByteArray {
    return ByteBuffer.allocate(4)
        .also { it.putInt(this) }.array()
        .let { it.sliceArray(it.size - capacity until it.size) }
}

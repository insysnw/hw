package net.fennmata.cnt.lab1.common

import java.nio.ByteBuffer

fun List<Byte>.toUnsignedNumber(): Long {
    return map { it.toLong() and 0xFF }.fold(0L) { acc, next -> (acc shl 8) or next }
}

fun List<Byte>.toUnsignedLong(): Long = toUnsignedNumber()
fun List<Byte>.toUnsignedInt(): Int = toUnsignedNumber().toInt()

fun ByteArray.toUnsignedLong(): Long = toList().toUnsignedLong()
fun ByteArray.toUnsignedInt(): Int = toList().toUnsignedInt()

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

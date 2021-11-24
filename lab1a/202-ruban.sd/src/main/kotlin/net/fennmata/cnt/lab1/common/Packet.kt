package net.fennmata.cnt.lab1.common

sealed class Packet(
    val state: Byte,
    val timestamp: Long,
    val clientName: String?
) {
    abstract val value1: Any?
    abstract val value2: Any?
}

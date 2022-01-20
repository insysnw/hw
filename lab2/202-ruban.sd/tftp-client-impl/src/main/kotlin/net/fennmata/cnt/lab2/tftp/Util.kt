package net.fennmata.cnt.lab2.tftp

import java.net.DatagramSocket
import java.net.SocketTimeoutException

fun <T> repeatUntil(predicate: (T) -> Boolean, action: () -> T): T {
    var result = action()
    while (!predicate(result)) result = action()
    return result
}

fun ByteArray.split(delimiter: Byte): List<ByteArray> {
    var last = 0
    val result = mutableListOf<ByteArray>()
    withIndex().filter { it.value == delimiter }.map { it.index }.forEach {
        println(it)
        result += sliceArray(last until it)
        last = it + 1
    }
    return result
}

fun DatagramSocket.repeatUntilFinishes(action: DatagramSocket.() -> Unit) {
    var didSocketTimeout = true
    while (didSocketTimeout) {
        try {
            action()
            didSocketTimeout = false
        } catch (e: SocketTimeoutException) {
            // ignore and go repeat action
        }
    }
}

package net.fennmata.cnt.lab2.tftp

import java.io.File
import java.net.DatagramSocket
import java.net.InetAddress

val udpSocket = DatagramSocket()

fun writeFile(host: InetAddress, file: File, mode: TransmissionMode) {
    TODO()
}

fun readFile(host: InetAddress, file: File, mode: TransmissionMode) {
    TODO()
}

fun main(arguments: Array<String>) {
    val command = arguments.getOrNull(0)?.let { str -> RequestType.valueOf(str.uppercase()) }
    checkNotNull(command) { "Illegal command" }

    val host = arguments.getOrNull(1)?.let { str -> InetAddress.getByName(str) }
    checkNotNull(host) { "Host address not provided" }

    val file = arguments.getOrNull(2)?.let { str -> File(str) }
    checkNotNull(file) { "Filename not provided" }
    check(file.exists()) { "Illegal filename provided" }

    val mode = arguments.getOrNull(3)?.let { str -> TransmissionMode.valueOf(str.uppercase()) }
        ?: TransmissionMode.OCTET

    when (command) {
        RequestType.WRITE -> writeFile(host, file, mode)
        RequestType.READ -> readFile(host, file, mode)
    }
}

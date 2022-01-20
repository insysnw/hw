package net.fennmata.cnt.lab2.tftp

import java.io.ByteArrayInputStream
import java.io.File
import java.net.DatagramSocket
import java.net.InetAddress

val udpSocket = DatagramSocket().apply { soTimeout = 2000 }

fun writeFile(host: InetAddress, file: File, mode: TransmissionMode) {
    udpSocket.writePacket(
        Request(RequestType.WRITE, file.name, mode),
        host, 69
    )

    var port = 0
    val answer = repeatUntil({ it is Acknowledgement || it is Error }) {
        udpSocket.readPacket(host) { port = it.port }
    }
    if (answer is Error) throw IllegalStateException(
        "server has answered with ${answer.code}: ${answer.message}"
    )

    var blockNumber: Short = 1
    val fileStream = when (mode) {
        TransmissionMode.NETASCII -> ByteArrayInputStream(file.readText().toASCII())
        TransmissionMode.OCTET -> file.inputStream()
    }
    repeatUntil({ it.size < 512 }) {
        val bytes = fileStream.readNBytes(512)

        val dataBlock = DataBlock(blockNumber, bytes)
        udpSocket.repeatUntilFinishes {
            writePacket(dataBlock, host, port)
            repeatUntil({ it is Acknowledgement && it.number == blockNumber }) {
                readPacket(host, port)
            }
            Unit
        }
        ++blockNumber

        bytes
    }
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
        RequestType.READ -> readFile(host, file, mode)
        RequestType.WRITE -> writeFile(host, file, mode)
    }
}

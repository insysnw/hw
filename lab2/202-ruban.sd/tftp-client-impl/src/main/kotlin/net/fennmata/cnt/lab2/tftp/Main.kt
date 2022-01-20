package net.fennmata.cnt.lab2.tftp

import java.io.ByteArrayInputStream
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

val udpSocket = DatagramSocket().apply { soTimeout = 2000 }

fun DatagramSocket.readPacketsUntil(
    predicate: (TFTPPacket) -> Boolean,
    host: InetAddress,
    port: Int? = null,
    processDatagramPacket: ((DatagramPacket) -> Unit)? = null
): TFTPPacket {
    val answer = repeatUntil({ predicate(it) || it is Error }) {
        readPacket(host, port, processDatagramPacket)
    }
    if (answer is Error) throw IllegalStateException(
        "server has answered with ${answer.code}: ${answer.message}"
    )
    return answer
}

fun writeFile(host: InetAddress, file: File, mode: TransmissionMode) {
    udpSocket.writePacket(
        Request(RequestType.WRITE, file.name, mode),
        host, 69
    )

    var port = 0
    var blockNumber: Short = 0
    udpSocket.readPacketsUntil(
        { it is Acknowledgement && it.number == blockNumber },
        host
    ) { port = it.port }

    val fileStream = when (mode) {
        TransmissionMode.NETASCII -> ByteArrayInputStream(file.readText().toASCII())
        TransmissionMode.OCTET -> file.inputStream()
    }
    repeatUntil({ it.size < 512 }) {
        ++blockNumber
        val bytes = fileStream.readNBytes(512)
        val dataBlock = DataBlock(blockNumber, bytes)
        udpSocket.repeatUntilFinishes {
            writePacket(dataBlock, host, port)
            readPacketsUntil(
                { it is Acknowledgement && it.number == blockNumber },
                host, port
            )
        }

        bytes
    }
    fileStream.close()
}

fun readFile(host: InetAddress, file: File, mode: TransmissionMode) {
    udpSocket.writePacket(
        Request(RequestType.READ, file.name, mode),
        host, 69
    )

    var port: Int? = null
    var blockNumber: Short = 0

    val fileStream by lazy { file.outputStream() }
    repeatUntil({ it.size < 512 }) {
        val response = udpSocket.repeatUntilFinishes {
            val portCopy = port
            if (portCopy != null) writePacket(Acknowledgement(blockNumber), host, portCopy)
            udpSocket.readPacketsUntil(
                { it is DataBlock && it.number == (blockNumber + 1).toShort() },
                host, port
            ) { port = it.port }
        }
        ++blockNumber
        val bytes = (response as DataBlock).data
        when (mode) {
            TransmissionMode.NETASCII -> fileStream.write(bytes.toString(Charsets.US_ASCII).encodeToByteArray())
            TransmissionMode.OCTET -> fileStream.write(bytes)
        }

        bytes
    }
    fileStream.close()
}

fun main(arguments: Array<String>) {
    val command = arguments.getOrNull(0)?.let { str -> RequestType.valueOf(str.uppercase()) }
    checkNotNull(command) { "Illegal command" }

    val host = arguments.getOrNull(1)?.let { str -> InetAddress.getByName(str) }
    checkNotNull(host) { "Host address not provided" }

    val file = arguments.getOrNull(2)?.let { str -> File(str) }
    checkNotNull(file) { "Filename not provided" }
    val fileParent = File(file.absolutePath.substringBeforeLast("/"))
    check(fileParent.exists() || fileParent.mkdir()) { "File directory does not exist" }
    if (command == RequestType.WRITE) check(file.exists()) { "Illegal filename provided" }

    val mode = arguments.getOrNull(3)?.let { str -> TransmissionMode.valueOf(str.uppercase()) }
        ?: TransmissionMode.OCTET

    when (command) {
        RequestType.READ -> readFile(host, file, mode)
        RequestType.WRITE -> writeFile(host, file, mode)
    }
}

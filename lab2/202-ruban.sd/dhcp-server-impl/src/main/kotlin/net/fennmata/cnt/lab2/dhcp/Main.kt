package net.fennmata.cnt.lab2.dhcp

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

val ipToAssign = byteArrayOf(48, 48, 48, 48)

const val serverPort = 67
const val clientPort = 68

lateinit var serverHost: InetAddress
val serverIP: ByteArray get() = serverHost.address

val broadcastAddress: InetAddress by lazy {
    InetAddress.getByName(if (serverHost.isLoopbackAddress) "127.255.255.255" else "255.255.255.255")
}

val udpSocket = DatagramSocket(serverPort)
val datagramBuffer = ByteArray(1024)

operator fun ByteArray.get(range: IntRange): ByteArray = sliceArray(range)

operator fun ByteArray.set(range: IntRange, value: ByteArray) {
    for (index in range) set(index, value[index - range.first])
}

fun Int.toByteArray(capacity: Int): ByteArray {
    return ByteBuffer.allocate(Int.SIZE_BYTES)
        .also { it.putInt(this) }.array()
        .let { it.sliceArray(it.size - capacity until it.size) }
}

fun main(arguments: Array<String>) {
    val serverHostValue = arguments.firstOrNull()?.let { str -> InetAddress.getByName(str) }
    checkNotNull(serverHostValue) { "Host address not provided" }
    serverHost = serverHostValue

    while (true) {
        val incomingPacket = DatagramPacket(datagramBuffer, datagramBuffer.size)
        udpSocket.receive(incomingPacket)

        println("incoming packet!")

        val packetBytes = datagramBuffer[0 until incomingPacket.length]
        println(packetBytes.toList())

        val data = packetBytes.sliceArray(0 until 240)
        val options = packetBytes.sliceArray(240 until packetBytes.size)

        data[0] = 2
        data[16 until 20] = ipToAssign
        data[20 until 24] = serverIP

        val incomingMessageType = options[options.indexOf(53) + 2]
        val outgoingMessageType = when (incomingMessageType.toInt()) {
            1 -> 2
            3 -> 5
            else -> throw IllegalStateException("Incoming DHCP operation is illegal or unsupported")
        }.toByte()

        val messageType = byteArrayOf(53, 1, outgoingMessageType)
        val serverIdentifier = byteArrayOf(54, 4) + serverIP
        val leaseTime = byteArrayOf(51, 4) + 86400.toByteArray(4)
        val outgoingMessage = data + messageType + serverIdentifier + leaseTime + byteArrayOf(255.toByte())

        println("outgoing package:")
        println(outgoingMessage.toList())

        val outgoingPacket = DatagramPacket(outgoingMessage, outgoingMessage.size, broadcastAddress, clientPort)
        udpSocket.send(outgoingPacket)

        println("response sent!")
    }
}

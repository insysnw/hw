package net.fennmata.cnt.lab2.tftp

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

private val byteBuffer = ByteArray(1024)

fun DatagramSocket.readPacket(
    host: InetAddress,
    port: Int? = null,
    processPacket: ((DatagramPacket) -> Unit)? = null
): TFTPPacket {
    val datagramPacket = DatagramPacket(byteBuffer, byteBuffer.size)
    while (true) {
        receive(datagramPacket)
        val isHostValid = host == datagramPacket.address
        val isPortValid = port == null || port == datagramPacket.port
        if (isHostValid && isPortValid) break
    }
    if (processPacket != null) processPacket(datagramPacket)
    return byteBuffer.sliceArray(0 until datagramPacket.length).toTFTPPacket()
}

fun DatagramSocket.writePacket(packet: TFTPPacket, host: InetAddress, port: Int) {
    val packetBytes = packet.toByteArray()
    val datagramPacket = DatagramPacket(packetBytes, packetBytes.size, host, port)
    send(datagramPacket)
}

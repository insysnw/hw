package net.fennmata.cnt.lab1.common

interface PacketTransceiver {
    fun transmit(receiver: NetworkHost, packet: Packet)
    fun receive(transmitter: NetworkHost): Packet
}

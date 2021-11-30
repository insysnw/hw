package net.fennmata.cnt.lab1.client

import net.fennmata.cnt.lab1.common.Application
import net.fennmata.cnt.lab1.common.ConnectionApproved
import net.fennmata.cnt.lab1.common.ConnectionNotification
import net.fennmata.cnt.lab1.common.ConnectionPacket
import net.fennmata.cnt.lab1.common.ConnectionRejected
import net.fennmata.cnt.lab1.common.ConnectionRequest
import net.fennmata.cnt.lab1.common.DisconnectionNotification
import net.fennmata.cnt.lab1.common.DisconnectionPacket
import net.fennmata.cnt.lab1.common.FilePacket
import net.fennmata.cnt.lab1.common.KeepAlivePacket
import net.fennmata.cnt.lab1.common.MessagePacket
import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.WarningOutput
import net.fennmata.cnt.lab1.common.readPacket
import net.fennmata.cnt.lab1.common.write
import net.fennmata.cnt.lab1.common.writePacket
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.time.OffsetDateTime

object ChatClient : Application<ChatClient>() {

    override val responses = listOf(
        QuitClientCommand,
        PrintClientHelpCommand
    )

    override fun initialize() {
        NotificationOutput.write("Please enter the server's IPv4 address.")
        val address = readln()
        NotificationOutput.write("Please enter the server's TCP port.")
        val port = readln().toInt()
        NotificationOutput.write("Please enter your username.")
        val requestedUsername = readln()

        socket.connect(InetSocketAddress(address, port))
        NotificationOutput.write("TCP connection to the server @ ${socket.remoteSocketAddress} has been successful.")

        val connectionRequest = ConnectionPacket(ConnectionRequest, OffsetDateTime.now(), requestedUsername)
        socket.writePacket(connectionRequest)
        NotificationOutput.write("A connection request has been sent to the server.")

        val serverAnswer = socket.readPacket()
        check(serverAnswer is ConnectionPacket) { "The server has replied incorrectly" }
        check(serverAnswer.state !is ConnectionRejected) { "The connection request was rejected by the server" }
        check(serverAnswer.state is ConnectionApproved) { "The server has replied incorrectly" }

        username = serverAnswer.clientName
        NotificationOutput.write("You have been connected to the chat as $username.", serverAnswer.timestamp)
    }

    override suspend fun execute() {
        while (isRunning) {
            val packet = try { socket.readPacket() } catch (e: SocketException) {
                WarningOutput.write("The connection to the server was closed [exception: ${e.message}].")
                close()
                null
            } ?: continue

            when (packet) {
                is ConnectionPacket -> process(packet)
                is DisconnectionPacket -> process(packet)
                is MessagePacket -> process(packet)
                is FilePacket -> process(packet)
                is KeepAlivePacket -> process(packet)
            }
        }
    }

    override fun finalize() {
        socket.close()
    }

    val socket = Socket()

    lateinit var username: String

    private fun process(packet: ConnectionPacket) = with(packet) {
        if (state !is ConnectionNotification) return@with
        NotificationOutput.write("User $clientName has connected to the chat.", timestamp)
    }

    private fun process(packet: DisconnectionPacket) = with(packet) {
        if (state !is DisconnectionNotification) return@with
        NotificationOutput.write("User $clientName has disconnected from the chat.", timestamp)
    }

    private fun process(packet: MessagePacket) = with(packet) {
        // TODO
    }

    private fun process(packet: FilePacket) = with(packet) {
        // TODO
    }

    private fun process(packet: KeepAlivePacket) = with(packet) {
        // TODO
    }

}

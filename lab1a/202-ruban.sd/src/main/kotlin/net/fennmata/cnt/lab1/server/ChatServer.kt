package net.fennmata.cnt.lab1.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.time.OffsetDateTime

object ChatServer : Application<ChatServer>() {

    override val responses = listOf(
        ShutdownServerCommand,
        PrintServerHelpCommand
    )

    override fun initialize() {
        NotificationOutput.write("Please enter the server's IPv4 address.")
        val address = readln()
        NotificationOutput.write("Please enter the server's TCP port.")
        val port = readln().toInt()
        serverSocket.bind(InetSocketAddress(address, port))
        NotificationOutput.write("The server is now running, bound to ${serverSocket.localSocketAddress}.")
    }

    override suspend fun execute() {
        while (isRunning) {
            NotificationOutput.write("Listening to new connections ...")
            val clientSocketDeferred = coroutineScope.async(Dispatchers.IO) {
                try { serverSocket.accept() } catch (e: SocketException) {
                    WarningOutput.write("The server socket was closed [exception: ${e.message}].")
                    close()
                    null
                }
            }
            val clientSocket = clientSocketDeferred.await() ?: return

            NotificationOutput.write("A TCP connection to ${clientSocket.remoteSocketAddress} was accepted.")

            val connectionRequest = try { clientSocket.readPacket() } catch (e: SocketException) {
                WarningOutput.write(
                    "Connection to ${clientSocket.remoteSocketAddress} was closed [exception: ${e.message}]."
                )
                null
            } ?: continue
            val timestamp = OffsetDateTime.now()

            var isRequestCorrect = true
            when {
                connectionRequest !is ConnectionPacket || connectionRequest.state !is ConnectionRequest -> {
                    NotificationOutput.write(
                        "The client @ ${clientSocket.remoteSocketAddress} has requested connection incorrectly."
                    )
                    isRequestCorrect = false
                }
                connectionRequest.clientName.isEmpty() || connectionRequest.clientName in clientUsernames -> {
                    NotificationOutput.write(
                        "The client @ ${clientSocket.remoteSocketAddress} has not provided a correct username."
                    )
                    isRequestCorrect = false
                }
            }
            if (!isRequestCorrect) {
                val connectionRejection = ConnectionPacket(ConnectionRejected, timestamp, connectionRequest.clientName)
                clientSocket.writePacket(connectionRejection)
                continue
            }

            val connectionApprovement = ConnectionPacket(ConnectionApproved, timestamp, connectionRequest.clientName)
            try { clientSocket.writePacket(connectionApprovement) } catch (e: SocketException) {
                WarningOutput.write(
                    "Connection to ${clientSocket.remoteSocketAddress} was closed [exception: ${e.message}]."
                )
                null
            } ?: continue
            connectClient(clientSocket, timestamp, connectionRequest.clientName)
            coroutineScope.launch(Dispatchers.Default) { clientSocket.serve() }
        }
    }

    override fun finalize() {
        serverSocket.close()
        clientSockets.forEach { it.close() }
    }

    private val serverSocket = ServerSocket()

    private val connections = mutableMapOf<Socket, String>()

    private val clientSockets get() = connections.keys

    private val clientUsernames get() = connections.values

    private fun <T> Iterable<T>.except(element: T) = filter { it != element }

    private fun connectClient(clientSocket: Socket, timestamp: OffsetDateTime, clientUsername: String) {
        connections[clientSocket] = clientUsername
        val connectionNotification = ConnectionPacket(ConnectionNotification, timestamp, clientUsername)
        clientSockets.except(clientSocket).forEach { it.writePacket(connectionNotification) }
    }

    private fun disconnectClient(clientSocket: Socket, timestamp: OffsetDateTime, clientUsername: String) {
        clientSocket.close()
        connections.remove(clientSocket)
        val disconnectionNotification = DisconnectionPacket(DisconnectionNotification, timestamp, clientUsername)
        clientSockets.forEach { it.writePacket(disconnectionNotification) }
    }

    private suspend fun Socket.serve() = coroutineScope {
        while (isActive) {
            val packet = try { readPacket() } catch (e: SocketException) {
                WarningOutput.write("Connection to $remoteSocketAddress was closed [exception: ${e.message}].")
                disconnectClient(
                    this@serve,
                    OffsetDateTime.now(),
                    connections[this@serve] ?: throw IllegalStateException("No username found for a client!")
                )
                cancel("Connection closed")
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

    private fun Socket.process(packet: ConnectionPacket) = with(packet) {
        val timestamp = OffsetDateTime.now()

        // TODO
    }

    private fun Socket.process(packet: DisconnectionPacket) = with(packet) {
        val timestamp = OffsetDateTime.now()

        // TODO
    }

    private fun Socket.process(packet: MessagePacket) = with(packet) {
        val timestamp = OffsetDateTime.now()

        // TODO
    }

    private fun Socket.process(packet: FilePacket) = with(packet) {
        val timestamp = OffsetDateTime.now()

        // TODO
    }

    private fun Socket.process(packet: KeepAlivePacket) = with(packet) {
        // TODO
    }

}

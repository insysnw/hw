package net.fennmata.cnt.lab1.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.fennmata.cnt.lab1.common.Application
import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.WarningOutput
import net.fennmata.cnt.lab1.common.readPacket
import net.fennmata.cnt.lab1.common.write
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

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
        NotificationOutput.write(
            "The server is now running, bound to ${serverSocket.localSocketAddress}."
        )
    }

    override suspend fun execute() {
        while (coroutineScope.isActive) {
            NotificationOutput.write("Listening to new connections ...")
            val clientSocketDeferred = coroutineScope.async(Dispatchers.IO) {
                try {
                    serverSocket.accept()
                } catch (e: SocketException) {
                    WarningOutput.write("The server socket was closed [exception: ${e.message}].")
                    close()
                    null
                }
            }
            val clientSocket = clientSocketDeferred.await() ?: return

            NotificationOutput.write("A connection to ${clientSocket.remoteSocketAddress} was accepted.")
            clientSockets += clientSocket
            coroutineScope.launch(Dispatchers.Default) { clientSocket.serve() }
        }
    }

    override fun finalize() {
        serverSocket.close()
        clientSockets.forEach { it.close() }
    }

    val clientSockets = mutableListOf<Socket>()

    private val serverSocket = ServerSocket()

    private suspend fun Socket.serve() = coroutineScope {
        fun Socket.stop() {
            close()
            clientSockets -= this
            cancel("Connection closed")
        }

        while (isActive) {
            val packetDeferred = coroutineScope.async(Dispatchers.IO) {
                try {
                    readPacket()
                } catch (e: SocketException) {
                    WarningOutput.write("Connection to $remoteSocketAddress was closed [exception: ${e.message}].")
                    stop()
                    null
                }
            }
            val packet = packetDeferred.await() ?: continue

            // TODO
        }
    }

}

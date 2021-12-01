package net.fennmata.cnt.lab1.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fennmata.cnt.lab1.common.Application
import net.fennmata.cnt.lab1.common.ConnectionApproved
import net.fennmata.cnt.lab1.common.ConnectionNotification
import net.fennmata.cnt.lab1.common.ConnectionPacket
import net.fennmata.cnt.lab1.common.ConnectionRejected
import net.fennmata.cnt.lab1.common.ConnectionRequest
import net.fennmata.cnt.lab1.common.DisconnectionNotification
import net.fennmata.cnt.lab1.common.DisconnectionPacket
import net.fennmata.cnt.lab1.common.FileDownloadRequest
import net.fennmata.cnt.lab1.common.FileNotification
import net.fennmata.cnt.lab1.common.FilePacket
import net.fennmata.cnt.lab1.common.FileTransferInfoPacket
import net.fennmata.cnt.lab1.common.FileTransferPacket
import net.fennmata.cnt.lab1.common.FileUploadRequest
import net.fennmata.cnt.lab1.common.KeepAlivePacket
import net.fennmata.cnt.lab1.common.MessagePacket
import net.fennmata.cnt.lab1.common.MessageSent
import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.WarningOutput
import net.fennmata.cnt.lab1.common.readPacketSafely
import net.fennmata.cnt.lab1.common.readable
import net.fennmata.cnt.lab1.common.write
import net.fennmata.cnt.lab1.common.writePacketSafely
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
                try {
                    serverSocket.accept()
                } catch (e: SocketException) {
                    WarningOutput.write("The server socket was closed [e: ${e.message}].")
                    close()
                    null
                }
            }
            val clientSocket = clientSocketDeferred.await() ?: return

            NotificationOutput.write("A TCP connection to ${clientSocket.remoteSocketAddress} was accepted.")

            val connectionRequest = clientSocket.readPacketSafely(coroutineScope) {
                WarningOutput.write("Connection to ${clientSocket.remoteSocketAddress} was closed [e: ${it.message}].")
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
                clientSocket.writePacketSafely(coroutineScope, connectionRejection) {
                    WarningOutput.write("Connection to ${clientSocket.remoteSocketAddress} was closed [e: ${it.message}].")
                }
                val deferred = coroutineScope.async(Dispatchers.IO) { clientSocket.close() }
                deferred.await()
                continue
            }

            val connectionApprovement = ConnectionPacket(ConnectionApproved, timestamp, connectionRequest.clientName)
            clientSocket.writePacketSafely(coroutineScope, connectionApprovement) {
                WarningOutput.write("Connection to ${clientSocket.remoteSocketAddress} was closed [e: ${it.message}].")
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

    private suspend fun connectClient(clientSocket: Socket, timestamp: OffsetDateTime, clientUsername: String) {
        connections[clientSocket] = clientUsername
        NotificationOutput.write("Client $clientUsername @ ${clientSocket.remoteSocketAddress} was connected.")
        val connectionNotification = ConnectionPacket(ConnectionNotification, timestamp, clientUsername)
        clientSockets.except(clientSocket).forEach {
            it.writePacketSafely(coroutineScope, connectionNotification) { e ->
                WarningOutput.write("Connection to ${it.remoteSocketAddress} was closed [e: ${e.message}].")
                disconnectClient(
                    it,
                    OffsetDateTime.now(),
                    connections[it] ?: throw IllegalStateException("No username found for a client")
                )
            }
        }
    }

    private suspend fun disconnectClient(clientSocket: Socket, timestamp: OffsetDateTime, clientUsername: String) {
        val deferred = coroutineScope.async(Dispatchers.IO) { clientSocket.close() }
        deferred.await()
        connections.remove(clientSocket)
        NotificationOutput.write("Client $clientUsername @ ${clientSocket.remoteSocketAddress} was disconnected.")
        val disconnectionNotification = DisconnectionPacket(DisconnectionNotification, timestamp, clientUsername)
        clientSockets.forEach {
            it.writePacketSafely(coroutineScope, disconnectionNotification) { e ->
                WarningOutput.write("Connection to ${it.remoteSocketAddress} was closed [e: ${e.message}].")
                disconnectClient(
                    it,
                    OffsetDateTime.now(),
                    connections[it] ?: throw IllegalStateException("No username found for a client")
                )
            }
        }
    }

    private suspend fun Socket.serve() = coroutineScope {
        var isServing = true

        suspend fun Socket.stop(e: SocketException) {
            if (!isServing) return
            isServing = false

            WarningOutput.write("Connection to $remoteSocketAddress was closed [e: ${e.message}].")
            disconnectClient(
                this@serve,
                OffsetDateTime.now(),
                connections[this@serve] ?: throw IllegalStateException("No username found for a client")
            )
            cancel("Connection closed")
        }

        while (isServing) {
            val keepAliveCheck = launch(Dispatchers.Default) {
                delay(2000L)
                stop(SocketException("Keep-alive check was failed by the client"))
            }

            val packet = readPacketSafely(this) { stop(it) } ?: continue
            keepAliveCheck.cancelAndJoin()

            when (packet) {
                is ConnectionPacket -> Unit
                is DisconnectionPacket -> Unit
                is MessagePacket -> process(packet)
                is FilePacket -> process(packet)
                is FileTransferInfoPacket -> Unit
                is FileTransferPacket -> Unit
                is KeepAlivePacket -> Unit
            }
        }
    }

    private suspend fun Socket.process(packet: MessagePacket) = with(packet) {
        val messageRetranslation = MessagePacket(
            MessageSent,
            timestamp = OffsetDateTime.now(),
            clientName = connections[this@process] ?: throw IllegalStateException("No username found for a client"),
            message = message
        )
        clientSockets.except(this@process).forEach {
            it.writePacketSafely(coroutineScope, messageRetranslation) { e ->
                WarningOutput.write("Connection to ${it.remoteSocketAddress} was closed [e: ${e.message}].")
                disconnectClient(
                    it,
                    OffsetDateTime.now(),
                    connections[it] ?: throw IllegalStateException("No username found for a client")
                )
            }
        }
    }

    private suspend fun Socket.process(packet: FilePacket) = with(packet) {
        val senderName = connections[this@process] ?: throw IllegalStateException("No username found for a client")
        when (packet.state) {
            is FileNotification -> return@with
            is FileUploadRequest -> {
                NotificationOutput.write(
                    "$senderName @ $remoteSocketAddress wants to upload $fileName (${fileLength.readable})."
                )
            }
            is FileDownloadRequest -> {
                NotificationOutput.write(
                    "$senderName @ $remoteSocketAddress wants to download $fileName by $clientName."
                )
            }
        }
    }

}

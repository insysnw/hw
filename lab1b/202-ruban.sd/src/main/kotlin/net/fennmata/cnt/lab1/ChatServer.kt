package net.fennmata.cnt.lab1

import net.fennmata.cnt.lab1.common.ConnectionAccepted
import net.fennmata.cnt.lab1.common.ConnectionNotification
import net.fennmata.cnt.lab1.common.ConnectionRequest
import net.fennmata.cnt.lab1.common.DisconnectionNotification
import net.fennmata.cnt.lab1.common.FileNotification
import net.fennmata.cnt.lab1.common.FileNotificationPending
import net.fennmata.cnt.lab1.common.FileSent
import net.fennmata.cnt.lab1.common.MessageNotification
import net.fennmata.cnt.lab1.common.MessageSent
import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.Packet
import net.fennmata.cnt.lab1.common.PacketBuffer
import net.fennmata.cnt.lab1.common.WarningOutput
import net.fennmata.cnt.lab1.common.toByteArray
import net.fennmata.cnt.lab1.common.write
import java.io.Closeable
import java.io.IOException
import java.lang.Exception
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.time.OffsetDateTime

object ChatServer : Runnable, Closeable {

    private val serverAddress: InetSocketAddress
    private val serverSelector = Selector.open()

    init {
        NotificationOutput.write("Please enter the server's IPv4 address.")
        val address = readln()
        NotificationOutput.write("Please enter the server's TCP port.")
        val port = readln().toInt()
        serverAddress = InetSocketAddress(address, port)

        val serverSocketChannel = ServerSocketChannel.open()
        serverSocketChannel.configureBlocking(false)
        serverSocketChannel.socket().bind(serverAddress)
        serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT)
        NotificationOutput.write("The server successfully began operation.")
    }

    private val byteBuffer: ByteBuffer

    init {
        var byteBufferCapacity = 0
        byteBufferCapacity += 2 // packet header
        byteBufferCapacity += 13 // maximum constant part of the packet body (see FileNotification)
        byteBufferCapacity += 256 // maximum width of the username packet field (see FileNotification)
        byteBufferCapacity += 256 // maximum width of the filename packet field (see FileNotification)
        byteBufferCapacity += 16777216 // maximum width of the file packet field (see FileNotification)
        byteBuffer = ByteBuffer.allocateDirect(byteBufferCapacity)
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        val result = mutableListOf<Byte>()
        for (i in 0 until position()) result += this[i]
        return result.toByteArray()
    }

    private val clients = mutableMapOf<SocketChannel, String?>()
    private val clientPacketBuffers = mutableMapOf<SocketChannel, PacketBuffer>()

    private val fileTransmissionServerSelector = Selector.open()
    private val activeFileTransmissions = mutableMapOf<ServerSocketChannel, Pair<OffsetDateTime, FileNotification>>()
    private val Pair<OffsetDateTime, FileNotification>.notificationTimeout get() = first
    private val Pair<OffsetDateTime, FileNotification>.fileNotification get() = second

    override fun run() {
        try {
            while (!isClosed) {
                activeFileTransmissions
                    .filter { (_, fileTransmission) -> OffsetDateTime.now() >= fileTransmission.notificationTimeout }
                    .forEach { (fileTransmissionServerSocketChannel, _) ->
                        fileTransmissionServerSocketChannel.close()
                        activeFileTransmissions.remove(fileTransmissionServerSocketChannel)
                    }
                if (fileTransmissionServerSelector.selectNow() != 0) {
                    val selectedKeysIterator = fileTransmissionServerSelector.selectedKeys().iterator()
                    while (selectedKeysIterator.hasNext()) {
                        val selectedKey = selectedKeysIterator.next()
                        selectedKeysIterator.remove()
                        if (!selectedKey.isValid) continue
                        if (selectedKey.isAcceptable) acceptFileTransmissionClient(selectedKey)
                        if (selectedKey.isReadable) disconnectFileTransmissionClient(selectedKey)
                    }
                }
                if (serverSelector.selectNow() != 0) {
                    val selectedKeysIterator = serverSelector.selectedKeys().iterator()
                    while (selectedKeysIterator.hasNext()) {
                        val selectedKey = selectedKeysIterator.next()
                        selectedKeysIterator.remove()
                        if (!selectedKey.isValid) continue
                        if (selectedKey.isAcceptable) acceptClient(selectedKey)
                        if (selectedKey.isReadable) readClientPacket(selectedKey)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            close()
        }
    }

    private fun acceptClient(key: SelectionKey) {
        try {
            val serverSocketChannel = key.channel() as ServerSocketChannel
            NotificationOutput.write("An accept event was received.")
            val clientChannel = serverSocketChannel.accept()
            if (clientChannel == null) {
                WarningOutput.write("A received accept event turned out to be false.")
                return
            }
            clientChannel.configureBlocking(false)
            clientChannel.register(serverSelector, SelectionKey.OP_READ)
            clients[clientChannel] = null
            NotificationOutput.write("A client @ ${clientChannel.socket().remoteSocketAddress} was accepted.")
        } catch (e: IOException) {
            WarningOutput.write("A new client wasn't accepted properly (see exception stacktrace).")
            e.printStackTrace()
        }
    }

    private fun readClientPacket(key: SelectionKey) {
        val clientChannel = key.channel() as SocketChannel
        NotificationOutput.write("A read event from ${clientChannel.socket().remoteSocketAddress} was received.")
        try {
            if (clientChannel.read(byteBuffer) == -1) {
                NotificationOutput.write("A client @ ${clientChannel.socket().remoteSocketAddress} closed the connection.")
                disconnectClient(clientChannel)
                return
            }
        } catch (e: IOException) {
            WarningOutput.write("A read from ${clientChannel.socket().remoteSocketAddress} was unsuccessful.")
            e.printStackTrace()
            disconnectClient(clientChannel)
            return
        }
        while (byteBuffer.position() != 0) {
            val clientPacketBuffer = clientPacketBuffers.getOrPut(clientChannel) { PacketBuffer() }
            clientPacketBuffer.put(byteBuffer.toByteArray())
            byteBuffer.clear()
            when (clientPacketBuffer.state) {
                PacketBuffer.State.PACKET_COMPLETE -> {
                    processClientPacket(clientChannel, clientPacketBuffer.toPacket())
                    byteBuffer.put(clientPacketBuffer.leftoverBytes)
                    clientPacketBuffers[clientChannel] = PacketBuffer()
                }
                PacketBuffer.State.PACKET_INCORRECT -> {
                    byteBuffer.put(clientPacketBuffer.leftoverBytes)
                    clientPacketBuffers[clientChannel] = PacketBuffer()
                }
                else -> Unit
            }
        }
    }

    private fun SocketChannel.writePacket(packet: Packet) {
        NotificationOutput.write("Sending a $packet packet to a client @ ${socket().remoteSocketAddress}.")
        byteBuffer.put(packet.toByteArray())
        byteBuffer.flip()
        var bytesSent = write(byteBuffer)
        while (bytesSent != 0) {
            Thread.sleep(4)
            bytesSent = write(byteBuffer)
        }
        byteBuffer.clear()
    }

    private fun disconnectClient(clientChannel: SocketChannel) {
        val timestamp = OffsetDateTime.now()
        NotificationOutput.write("A client @ ${clientChannel.socket().remoteSocketAddress} is to be disconnected.")
        clientChannel.close()
        val username = clients.remove(clientChannel) ?: return
        val disconnectionNotification = DisconnectionNotification(timestamp, username)
        clients.forEach { (clientToNotifyChannel, clientToNotifyUsername) ->
            if (clientToNotifyUsername == null) return@forEach
            clientToNotifyChannel.writePacket(disconnectionNotification)
        }
        NotificationOutput.write("All named clients were notified about disconnection of \"$username\".")
    }

    private fun processClientPacket(clientChannel: SocketChannel, packet: Packet) = when (packet) {
        is ConnectionRequest -> connectClient(clientChannel, packet)
        is MessageSent -> processMessage(clientChannel, packet)
        is FileSent -> processFile(clientChannel, packet)
        else -> Unit // ignoring all the other packets silently
    }

    private fun connectClient(clientChannel: SocketChannel, connectionRequest: ConnectionRequest) {
        val timestamp = OffsetDateTime.now()
        val username = connectionRequest.username
        NotificationOutput.write("A client @ ${clientChannel.socket().remoteSocketAddress} connected as \"$username\".")
        clientChannel.writePacket(ConnectionAccepted)
        val connectionNotification = ConnectionNotification(timestamp, username)
        clients.forEach { (clientToNotifyChannel, clientToNotifyUsername) ->
            if (clientToNotifyUsername == null) return@forEach
            clientToNotifyChannel.writePacket(connectionNotification)
        }
        NotificationOutput.write("All named clients were notified about connection of \"$username\".")
        clients[clientChannel] = username
    }

    private fun processMessage(clientChannel: SocketChannel, messageSent: MessageSent) {
        val timestamp = OffsetDateTime.now()
        val username = clients[clientChannel]
        if (username == null) {
            NotificationOutput.write(
                "An unnamed client @ ${clientChannel.socket().remoteSocketAddress} sent a message. It will be ignored."
            )
            return
        }

        val message = messageSent.message
        NotificationOutput.write("\"$username\" @ ${clientChannel.socket().remoteSocketAddress} sent a message \"$message\".")

        val messageNotification = MessageNotification(timestamp, username, message)
        clients.forEach { (clientToNotifyChannel, clientToNotifyUsername) ->
            if (clientToNotifyUsername == null) return@forEach
            clientToNotifyChannel.writePacket(messageNotification)
        }
        NotificationOutput.write("The incoming message notification was sent to all named clients.")
    }

    private fun processFile(clientChannel: SocketChannel, fileSent: FileSent) {
        val timestamp = OffsetDateTime.now()
        val username = clients[clientChannel]
        if (username == null) {
            NotificationOutput.write(
                "An unnamed client @ ${clientChannel.socket().remoteSocketAddress} sent a file. It will be ignored."
            )
            return
        }

        val filename = fileSent.filename
        val file = fileSent.file
        NotificationOutput.write("\"$username\" @ ${clientChannel.socket().remoteSocketAddress} sent a file \"$filename\".")

        val fileTransmissionServerSocketChannel = ServerSocketChannel.open()
        fileTransmissionServerSocketChannel.configureBlocking(false)
        fileTransmissionServerSocketChannel.bind(InetSocketAddress(serverAddress.address, 0))
        fileTransmissionServerSocketChannel.register(fileTransmissionServerSelector, SelectionKey.OP_ACCEPT)
        val fileNotification = FileNotification(timestamp, username, filename, file)
        val notificationTimeout = OffsetDateTime.now().plusMinutes(2)
        activeFileTransmissions[fileTransmissionServerSocketChannel] = notificationTimeout to fileNotification
        NotificationOutput.write(
            "${fileTransmissionServerSocketChannel.socket().localSocketAddress} is now listening for \"$filename\" transmissions."
        )

        val fileNotificationPending = FileNotificationPending(
            fileTransmissionServerSocketChannel.socket().localPort,
            notificationTimeout
        )
        clients.forEach { (clientToNotifyChannel, clientToNotifyUsername) ->
            if (clientToNotifyUsername == null || clientToNotifyUsername == username) return@forEach
            clientToNotifyChannel.writePacket(fileNotificationPending)
        }
        NotificationOutput.write("The incoming file notification was sent to all named clients.")
    }

    private fun acceptFileTransmissionClient(key: SelectionKey) {
        try {
            val fileTransmissionServerSocketChannel = key.channel() as ServerSocketChannel

            val relevantFileTransmission = activeFileTransmissions[fileTransmissionServerSocketChannel]
            checkNotNull(relevantFileTransmission) { "A port was opened without assigning a file transmission to it" }

            NotificationOutput.write("A file transmission accept event was received.")
            fileTransmissionServerSocketChannel.accept()?.let { fileTransmissionClientChannel ->
                fileTransmissionClientChannel.configureBlocking(false)
                fileTransmissionClientChannel.register(fileTransmissionServerSelector, SelectionKey.OP_READ)
                fileTransmissionClientChannel.writePacket(relevantFileTransmission.fileNotification)
            } ?: run {
                WarningOutput.write("A received file transmission accept event turned out to be false.")
            }
        } catch (e: IOException) {
            WarningOutput.write("A file transmission client wasn't served properly (see exception stacktrace).")
            e.printStackTrace()
        }
    }

    private fun disconnectFileTransmissionClient(key: SelectionKey) {
        NotificationOutput.write("A file transmission read event was received. Closing the corresponding connection ...")
        (key.channel() as SocketChannel).close()
    }

    private var isClosed = false

    override fun close() {
        if (isClosed) return
        isClosed = true
        if (serverSelector.isOpen) {
            serverSelector.keys().forEach {
                val channel = it.channel()
                channel.close()
                if (channel is SocketChannel) clients.remove(channel)
            }
            serverSelector.close()
        }
        if (fileTransmissionServerSelector.isOpen) {
            fileTransmissionServerSelector.keys().forEach {
                val channel = it.channel()
                channel.close()
                if (channel is ServerSocketChannel) activeFileTransmissions.remove(channel)
            }
            fileTransmissionServerSelector.close()
        }
    }

}

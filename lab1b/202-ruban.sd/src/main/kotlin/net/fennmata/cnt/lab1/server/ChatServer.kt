package net.fennmata.cnt.lab1.server

import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.Packet
import net.fennmata.cnt.lab1.common.PacketBuffer
import net.fennmata.cnt.lab1.common.WarningOutput
import net.fennmata.cnt.lab1.common.write
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

object ChatServer : Runnable, Closeable {

    private val fileStorage = File("files")

    init {
        check(fileStorage.deleteRecursively()) { "Directory \"${fileStorage.absoluteFile}\" wasn't cleared properly" }
        check(fileStorage.mkdirs()) { "Directory \"${fileStorage.absoluteFile}\" wasn't created properly" }
    }

    private val serverSelector = Selector.open()

    init {
        NotificationOutput.write("Please enter the server's IPv4 address.")
        val address = readln()
        NotificationOutput.write("Please enter the server's TCP port.")
        val port = readln().toInt()

        val serverSocketChannel = ServerSocketChannel.open()
        serverSocketChannel.configureBlocking(false)
        serverSocketChannel.socket().bind(InetSocketAddress(address, port))
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

    private val clients = mutableMapOf<SocketChannel, String?>()
    private val clientPacketBuffers = mutableMapOf<SocketChannel, PacketBuffer>()

    override fun run() {
        try {
            while (!isClosed) {
                if (serverSelector.selectNow() == 0) continue
                val selectedKeysIterator = serverSelector.selectedKeys().iterator()
                while (selectedKeysIterator.hasNext()) {
                    val selectedKey = selectedKeysIterator.next()
                    selectedKeysIterator.remove()
                    if (!selectedKey.isValid) continue
                    if (selectedKey.isAcceptable) acceptClient(selectedKey)
                    if (selectedKey.isReadable) readClientPacket(selectedKey)
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
        if (clientChannel.read(byteBuffer) == -1) {
            NotificationOutput.write("A client @ ${clientChannel.socket().remoteSocketAddress} closed the connection.")
            disconnectClient(clientChannel)
            return
        }
        // TODO an actual packet read
    }

    private fun disconnectClient(clientChannel: SocketChannel) {
        clientChannel.close()
        clients.remove(clientChannel)
        // TODO send disconnection notifications to remaining clients
    }

    private fun processClientPacket(clientChannel: SocketChannel, packet: Packet) {
        TODO()
    }

    private var isClosed = false

    override fun close() {
        isClosed = true
        if (serverSelector.isOpen) {
            serverSelector.keys().forEach {
                val channel = it.channel()
                channel.close()
                if (channel is SocketChannel) clients.remove(channel)
            }
            serverSelector.close()
        }
    }

}

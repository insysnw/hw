package net.fennmata.cnt.lab1.client

import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.write
import net.fennmata.cnt.lab1.server.ChatServer
import java.io.Closeable
import java.io.File
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.Socket

object ChatClient : Runnable, Closeable {

    private val fileStorage = File("files")

    init {
        check(fileStorage.deleteRecursively()) { "Directory \"${fileStorage.absoluteFile}\" wasn't cleared properly" }
        check(fileStorage.mkdirs()) { "Directory \"${fileStorage.absoluteFile}\" wasn't created properly" }
    }

    private val username: String

    init {
        NotificationOutput.write("Please enter your username.")
        username = readln()
    }

    private val socket = Socket()

    init {
        NotificationOutput.write("Please enter the server's IPv4 address.")
        val address = readln()
        NotificationOutput.write("Please enter the server's TCP port.")
        val port = readln().toInt()

        socket.connect(InetSocketAddress(address, port))
        NotificationOutput.write("TCP connection to the server @ ${socket.remoteSocketAddress} was successful.")
    }

    override fun run() {
        try {
            // TODO command read coroutine
            while (!isClosed) {
                // TODO socket read loop
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ChatServer.close()
        }
    }

    private var isClosed = false

    override fun close() {
        isClosed = true
        socket.close()
        // TODO command read coroutine
    }

}

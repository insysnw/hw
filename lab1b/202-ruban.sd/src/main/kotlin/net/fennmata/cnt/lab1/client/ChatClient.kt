package net.fennmata.cnt.lab1.client

import net.fennmata.cnt.lab1.common.ConnectionAccepted
import net.fennmata.cnt.lab1.common.ConnectionNotification
import net.fennmata.cnt.lab1.common.ConnectionRejected
import net.fennmata.cnt.lab1.common.ConnectionRequest
import net.fennmata.cnt.lab1.common.DisconnectionNotification
import net.fennmata.cnt.lab1.common.FileNotificationPending
import net.fennmata.cnt.lab1.common.MessageNotification
import net.fennmata.cnt.lab1.common.MessageOutput
import net.fennmata.cnt.lab1.common.MessageSent
import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.WarningOutput
import net.fennmata.cnt.lab1.common.readPacket
import net.fennmata.cnt.lab1.common.write
import net.fennmata.cnt.lab1.common.writePacket
import java.io.Closeable
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.time.OffsetDateTime

object ChatClient : Runnable, Closeable {

    private val fileStorage = File("files")

    init {
        check(fileStorage.deleteRecursively()) { "Directory \"${fileStorage.absoluteFile}\" wasn't cleared properly" }
        check(fileStorage.mkdirs()) { "Directory \"${fileStorage.absoluteFile}\" wasn't created properly" }
    }

    private val username: String = run {
        NotificationOutput.write("Please enter your username.")
        readln()
    }

    private val socket = Socket()

    init {
        NotificationOutput.write("Please enter the server's IPv4 address.")
        val address = readln()
        NotificationOutput.write("Please enter the server's TCP port.")
        val port = readln().toInt()

        socket.connect(InetSocketAddress(address, port))
        NotificationOutput.write("TCP connection to the server @ ${socket.remoteSocketAddress} was successful.")

        try {
            socket.writePacket(ConnectionRequest(username))
            when (socket.readPacket()) {
                ConnectionAccepted -> {
                    NotificationOutput.write(
                        "You successfully connected to the chat as \"$username\".",
                        OffsetDateTime.now()
                    )
                }
                ConnectionRejected -> throw IllegalStateException("The server rejected your connection request")
                else -> throw IllegalStateException("The server sent an illegal response")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            close()
        }
    }

    override fun run() {
        try {
            CommandReadThread.start()
            while (!isClosed) {
                when (val packet = socket.readPacket()) {
                    is ConnectionNotification -> {
                        NotificationOutput.write(
                            "The user \"${packet.username}\" connected to the chat.",
                            packet.timestamp
                        )
                    }
                    is DisconnectionNotification -> {
                        NotificationOutput.write(
                            "The user \"${packet.username}\" disconnected from the chat.",
                            packet.timestamp
                        )
                    }
                    is MessageNotification -> {
                        MessageOutput.write(packet.message, packet.timestamp, packet.username)
                    }
                    is FileNotificationPending -> {
                        TODO("implementation")
                    }
                    else -> Unit // ignoring all the other packets silently
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            close()
        }
    }

    private object CommandReadThread : Thread() {
        override fun run() {
            try {
                while (!isInterrupted) {
                    val command = readln().toCommand()
                    if (isInterrupted) continue
                    command()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                close()
            }
        }

        private sealed interface Command {
            operator fun invoke()
        }
        private class MessageCommand(private val message: String) : Command {
            override fun invoke() {
                socket.writePacket(MessageSent(message))
            }
        }
        private class FileCommand(private val filename: String) : Command {
            override fun invoke() {
                TODO("implementation")
            }
        }

        private fun String.toCommand(): Command {
            return if (startsWith("/file")) {
                FileCommand(substringAfter("/file").trim())
            } else {
                MessageCommand(this)
            }
        }
    }

    private var isClosed = false

    override fun close() {
        if (isClosed) return
        isClosed = true
        socket.close()
        CommandReadThread.interrupt()
        WarningOutput.write("The client has finished working. Press Enter if necessary to exit.")
    }

}

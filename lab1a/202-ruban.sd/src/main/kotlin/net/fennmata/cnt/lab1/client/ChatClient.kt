package net.fennmata.cnt.lab1.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import net.fennmata.cnt.lab1.common.Application
import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.WarningOutput
import net.fennmata.cnt.lab1.common.readPacket
import net.fennmata.cnt.lab1.common.write
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException

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
        socket.connect(InetSocketAddress(address, port))
        NotificationOutput.write(
            "TCP connection to the server @ ${socket.remoteSocketAddress} has been successful."
        )
    }

    override suspend fun execute() {
        while (coroutineScope.isActive) {
            val packetDeferred = coroutineScope.async(Dispatchers.IO) {
                try {
                    socket.readPacket()
                } catch (e: SocketException) {
                    WarningOutput.write("The connection to the server was closed [exception: ${e.message}].")
                    close()
                    null
                }
            }
            val packet = packetDeferred.await() ?: continue

            // TODO
        }
    }

    override fun finalize() {
        socket.close()
    }

    val socket = Socket()

}

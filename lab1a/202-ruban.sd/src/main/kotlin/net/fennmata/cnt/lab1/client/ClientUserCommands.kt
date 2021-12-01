package net.fennmata.cnt.lab1.client

import net.fennmata.cnt.lab1.common.ApplicationResponse
import net.fennmata.cnt.lab1.common.MessageOutput
import net.fennmata.cnt.lab1.common.MessagePacket
import net.fennmata.cnt.lab1.common.MessageSent
import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.WarningOutput
import net.fennmata.cnt.lab1.common.write
import net.fennmata.cnt.lab1.common.writePacketSafely
import java.time.OffsetDateTime

object QuitClientCommand : ApplicationResponse<ChatClient> {
    override val command = "quit"
    override suspend fun execute(arg: String?) {
        NotificationOutput.write("Stopping the execution ...")
        ChatClient.close()
    }
}

object PrintClientHelpCommand : ApplicationResponse<ChatClient> {
    override val command = "help"
    override suspend fun execute(arg: String?) {
        TODO()
    }
}

object SendMessage : ApplicationResponse<ChatClient> {
    override val command = "send"
    override suspend fun execute(arg: String?) {
        val timestamp = OffsetDateTime.now()
        val message = arg ?: ""
        val messagePacket = MessagePacket(MessageSent, timestamp, ChatClient.username, message)
        ChatClient.socket.writePacketSafely(ChatClient.coroutineScope, messagePacket) {
            WarningOutput.write("The connection to the server was closed [e: ${it.message}].")
            ChatClient.close()
        }
        MessageOutput.write(message, timestamp, ChatClient.username)
    }
}

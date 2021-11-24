package net.fennmata.cnt.lab1.server

import kotlinx.coroutines.delay
import net.fennmata.cnt.lab1.common.Application
import net.fennmata.cnt.lab1.common.MessageChatOutput
import net.fennmata.cnt.lab1.common.write

object ChatServer : Application<ChatServer>() {

    override val commands = listOf(
        ShutdownServerCommand,
        PrintServerHelpCommand
    )

    override fun initialize() {
        // TODO
    }

    override suspend fun update() {
        delay(2500)
        MessageChatOutput.write("Let's pretend this is a message.")
    }

}

package net.fennmata.cnt.lab1.client

import net.fennmata.cnt.lab1.common.ApplicationResponse
import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.write

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

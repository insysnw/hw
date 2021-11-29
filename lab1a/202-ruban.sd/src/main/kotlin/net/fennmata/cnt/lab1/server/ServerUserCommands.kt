package net.fennmata.cnt.lab1.server

import net.fennmata.cnt.lab1.common.ApplicationResponse
import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.write

object ShutdownServerCommand : ApplicationResponse<ChatServer> {
    override val command = "shutdown"
    override suspend fun execute(arg: String?) {
        NotificationOutput.write("Stopping the execution ...")
        ChatServer.close()
    }
}

object PrintServerHelpCommand : ApplicationResponse<ChatServer> {
    override val command = "help"
    override suspend fun execute(arg: String?) {
        TODO()
    }
}

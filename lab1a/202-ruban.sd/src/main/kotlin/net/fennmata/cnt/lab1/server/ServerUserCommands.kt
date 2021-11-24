package net.fennmata.cnt.lab1.server

import net.fennmata.cnt.lab1.common.Application
import net.fennmata.cnt.lab1.common.CustomUserCommand
import net.fennmata.cnt.lab1.common.NotificationChatOutput
import net.fennmata.cnt.lab1.common.write

object ShutdownServerCommand : CustomUserCommand<ChatServer> {
    override val name = "shutdown"
    override suspend fun executeOn(application: Application<ChatServer>, arg: String?) {
        NotificationChatOutput.write("Stopping the execution ...")
        application.finish()
    }
}

object PrintServerHelpCommand : CustomUserCommand<ChatServer> {
    override val name = "help"
    override suspend fun executeOn(application: Application<ChatServer>, arg: String?) {
        TODO()
    }
}

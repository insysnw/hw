package net.fennmata.cnt.lab1

import kotlinx.coroutines.delay
import net.fennmata.cnt.lab1.common.Application
import net.fennmata.cnt.lab1.common.CustomUserCommand
import net.fennmata.cnt.lab1.common.MessageChatOutput
import net.fennmata.cnt.lab1.common.NotificationChatOutput
import net.fennmata.cnt.lab1.common.write

object ShutdownTestCommand : CustomUserCommand<TestApplication> {
    override val name = "shutdown"
    override suspend fun executeOn(application: Application<TestApplication>, arg: String?) {
        NotificationChatOutput.write("Stopping the execution ...")
        application.finish()
    }
}

object TestApplication : Application<TestApplication>() {

    override val commands = listOf(ShutdownTestCommand)

    override fun initialize() = Unit

    override suspend fun update() {
        delay(4000)
        MessageChatOutput.write("Let's pretend this is a message.")
    }

}

fun main() {
    TestApplication.run()
}

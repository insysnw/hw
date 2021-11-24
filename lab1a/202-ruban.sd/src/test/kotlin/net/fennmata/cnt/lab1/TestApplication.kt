package net.fennmata.cnt.lab1

import kotlinx.coroutines.delay
import net.fennmata.cnt.lab1.common.Application
import net.fennmata.cnt.lab1.common.CustomUserCommand
import net.fennmata.cnt.lab1.common.MessageChatOutput
import net.fennmata.cnt.lab1.common.write

object TestApplication : Application<TestApplication>() {

    override val commands = emptyList<CustomUserCommand<TestApplication>>()

    override suspend fun update() {
        delay(2000)
        MessageChatOutput.write("Let's pretend this is a message.")
    }

}

fun main() {
    TestApplication.run()
}

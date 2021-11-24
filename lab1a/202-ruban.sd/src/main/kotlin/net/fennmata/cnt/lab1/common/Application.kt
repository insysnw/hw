package net.fennmata.cnt.lab1.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

abstract class Application<T : Application<T>> : Runnable {

    protected abstract val commands: List<CustomUserCommand<T>>

    protected abstract fun initialize()

    protected abstract suspend fun update()

    final override fun run() {
        initialize()
        NotificationChatOutput.write("Available commands: ${commands.map { command -> command.name }}")
        try {
            runBlocking {
                mainScope = this
                launch(Dispatchers.IO) { while (isActive) read() }
                while (isActive) update()
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                println()
                e.printStackTrace()
            }
        } finally {
            NotificationChatOutput.write("The process has stopped its execution. Goodbye.")
        }
    }

    fun finish() { mainScope.cancel() }

    private lateinit var mainScope: CoroutineScope

    private val unknownCommand = UnknownUserCommand<T>()

    private val noUserCommand = NoUserCommand<T>()

    private suspend fun read() = coroutineScope {
        val input = readln().trim().replace(Regex("\\s+"), " ")
        val (commandName, commandArg) = if (' ' in input)
            input.substringBefore(' ') to input.substringAfter(' ')
        else
            input to null
        yield()

        if (commandName.isEmpty()) {
            noUserCommand.executeOn(this@Application)
        } else {
            val command = commands.find { command -> command.name == commandName } ?: unknownCommand
            command.executeOn(this@Application, commandArg)
        }
    }

}

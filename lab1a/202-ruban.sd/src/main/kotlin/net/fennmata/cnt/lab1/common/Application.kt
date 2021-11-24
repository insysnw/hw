package net.fennmata.cnt.lab1.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

abstract class Application<T : Application<T>> : Runnable {

    protected abstract val commands: List<CustomUserCommand<T>>

    protected abstract suspend fun update()

    final override fun run() {
        runBlocking {
            println("Available commands: ${commands.map { command -> command.name }}")
            print("$awaitingInputMarker ")
            launch(Dispatchers.IO) { processInput() }
            while (!isFinished) { update() }
        }
    }

    protected var isFinished = false

    private val unknownCommand = UnknownUserCommand<T>()

    private val noUserCommand = NoUserCommand<T>()

    private suspend fun processInput() = coroutineScope {
        while (!isFinished) {
            val input = readln().trim().replace(Regex("\\s+"), " ")
            val (commandName, commandArg) = if (' ' in input)
                input.substringBefore(' ') to input.substringAfter(' ')
            else
                input to null
            if (commandName.isEmpty()) {
                noUserCommand.executeOn(this@Application)
            } else {
                val command = commands.find { command -> command.name == commandName } ?: unknownCommand
                command.executeOn(this@Application, commandArg)
            }
        }
    }

}

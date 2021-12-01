package net.fennmata.cnt.lab1.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.io.Closeable

abstract class Application<T : Application<T>> : Runnable, Closeable {

    protected abstract val responses: List<ApplicationResponse<T>>

    protected abstract fun initialize()

    protected abstract suspend fun execute()

    protected abstract fun finalize()

    final override fun run() {
        initialize()
        NotificationOutput.write("Available commands: ${responses.map { it.command }}")
        isRunning = true

        try {
            runBlocking {
                coroutineScope = this

                coroutineScope.launch(Dispatchers.Default) { execute() }

                while (isActive) {
                    val input = readln().trim().replace(Regex("\\s+"), " ")
                    val (commandName, commandArg) = if (' ' in input)
                        input.substringBefore(' ') to input.substringAfter(' ')
                    else
                        input to null

                    yield()

                    if (commandName.isEmpty()) {
                        noCommand.execute()
                    } else {
                        val command = responses.find { it.command == commandName } ?: unknownCommand
                        command.execute(commandArg)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException)
                WarningOutput.write("Coroutines were cancelled [e: ${e.message}].")
            else
                e.printStackTrace()
        } finally {
            finalize()
        }

        NotificationOutput.write("The application has finished its execution. Goodbye.")
    }

    final override fun close() {
        if (isRunning) {
            isRunning = false

            finalize()

            coroutineScope.cancel("Application closed")
            NotificationOutput.write("The application is finishing its execution. Press Enter if required.")
        }
    }

    lateinit var coroutineScope: CoroutineScope

    protected var isRunning = false

    private val unknownCommand = object : ApplicationBaseResponse<T> {
        override suspend fun execute(arg: String?) {
            WarningOutput.write("Unknown command.")
        }
    }

    private val noCommand = object : ApplicationBaseResponse<T> {
        override suspend fun execute(arg: String?) {
            WarningOutput.write("Please enter a command.")
        }
    }

}

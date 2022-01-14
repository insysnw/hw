package net.fennmata.cnt.lab1

fun main(arguments: Array<String>) {
    when (val applicationType = arguments.firstOrNull()?.lowercase()) {
        null -> println("Please specify the application type: \"client\" or \"server\".")
        "client" -> ChatClient.use { it.run() }
        "server" -> ChatServer.use { it.run() }
        else -> println("No application type such as \"$applicationType\": try \"client\" or \"server\".")
    }
}

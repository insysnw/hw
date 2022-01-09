package net.fennmata.cnt.lab1

import net.fennmata.cnt.lab1.client.ChatClient
import net.fennmata.cnt.lab1.server.ChatServer

fun main(arguments: Array<String>) {
    when (val applicationType = arguments.firstOrNull()?.lowercase()) {
        null -> println("Please specify the application type: \"client\" or \"server\".")
        "client" -> ChatClient.run()
        "server" -> ChatServer.run()
        else -> println("No application type such as \"$applicationType\": try \"client\" or \"server\".")
    }
}

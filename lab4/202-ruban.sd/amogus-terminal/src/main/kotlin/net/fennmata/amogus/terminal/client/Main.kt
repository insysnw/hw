package net.fennmata.amogus.terminal.client

import net.fennmata.amogus.terminal.client.apis.MainApi
import net.fennmata.amogus.terminal.client.infrastructure.ApiClient
import net.fennmata.amogus.terminal.client.models.FilesList
import net.fennmata.amogus.terminal.client.models.KillResult
import net.fennmata.amogus.terminal.client.models.MoveTo
import net.fennmata.amogus.terminal.client.models.Notification
import net.fennmata.amogus.terminal.client.models.Query
import net.fennmata.amogus.terminal.client.models.Role
import net.fennmata.amogus.terminal.client.models.UsersList

fun main() {
    val apiInstance = MainApi("http://localhost:6969")
    println("amogus terminal client v. -1.0.0")

    val (role, location, identity) = apiInstance.getNewUser()
    ApiClient.apiKey["Identity"] = identity
    println("your role: ${role.title}")
    println("available commands: ${role.allowedCommands}")
    println("current location: ${location.location}")

    while (true) {
        print("> ")
        val command = readln()
        val status = apiInstance.getMyself()
        if (!status.isAlive) {
            println("sorry, you're already dead")
            break
        }
        val query = Query(command.split(" "))
        when (val response = apiInstance.postQuery(query)) {
            is KillResult -> println("${response.killedUsersCount} users have been killed")
            is Role -> {
                println("your new role: ${response.title}")
                println("available commands: ${response.allowedCommands}")
            }
            is FilesList -> response.files.forEach { file -> println("\t$file") }
            is UsersList -> response.users.forEach { user -> println("\t${user.user} @ ${user.location}") }
            is Notification -> println(response.message)
            is MoveTo -> println("new location: ${response.location}")
        }
    }
}

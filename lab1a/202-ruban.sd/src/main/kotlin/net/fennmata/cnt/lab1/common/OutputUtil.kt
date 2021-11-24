package net.fennmata.cnt.lab1.common

import java.time.OffsetTime

sealed class ChatOutput(val marker: String)

object NotificationChatOutput : ChatOutput(":")
object ErrorChatOutput : ChatOutput("!")
object MessageChatOutput : ChatOutput("m")
object FileChatOutput : ChatOutput("f")

fun ChatOutput.write(text: String, timestamp: OffsetTime? = null, author: String? = null) {
    val output = StringBuilder(" ".repeat(4)).apply {
        if (timestamp != null) append("[${timestamp.hour}:${timestamp.minute}:${timestamp.second}] ")
        if (author != null) append("$author ")
        append("$marker> $text")
    }.toString()
    println(output)
}

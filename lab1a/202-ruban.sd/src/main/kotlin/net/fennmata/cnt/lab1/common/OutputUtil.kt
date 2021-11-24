package net.fennmata.cnt.lab1.common

import java.time.OffsetTime

sealed class ChatOutput(val marker: String)
object ErrorChatOutput : ChatOutput("!")
object MessageChatOutput : ChatOutput("m")

const val awaitingInputMarker = "?>"

fun ChatOutput.write(text: String, timestamp: OffsetTime? = null, author: String? = null) {
    val output = StringBuilder("\r").apply {
        if (timestamp != null) append("[${timestamp.hour}:${timestamp.minute}:${timestamp.second}] ")
        if (author != null) append("$author ")
        append("$marker> $text")
    }.toString()
    println(output)
    print("$awaitingInputMarker ")
}

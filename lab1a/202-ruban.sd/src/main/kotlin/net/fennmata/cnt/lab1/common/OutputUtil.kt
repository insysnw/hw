package net.fennmata.cnt.lab1.common

import java.time.OffsetDateTime

sealed class ApplicationOutput(val marker: String)

object NotificationOutput : ApplicationOutput(":")
object WarningOutput : ApplicationOutput("!")
object MessageOutput : ApplicationOutput("m")
object FileOutput : ApplicationOutput("f")

fun ApplicationOutput.write(text: String, timestamp: OffsetDateTime? = null, author: String? = null) {
    val output = StringBuilder(" ".repeat(4)).apply {
        if (timestamp != null) append("[${timestamp.hour}:${timestamp.minute}:${timestamp.second}] ")
        if (author != null) append("$author ")
        append("$marker> $text")
    }.toString()
    println(output)
}

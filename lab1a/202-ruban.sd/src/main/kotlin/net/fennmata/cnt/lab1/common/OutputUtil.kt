package net.fennmata.cnt.lab1.common

import java.text.DecimalFormat
import java.time.OffsetDateTime
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.min
import kotlin.math.pow

private val possibleFileSizeUnits = listOf("bytes", "Kbytes", "Mbytes", "Gbytes")

/**
 * To be used with file sizes.
 */
val Int.readable: String get() {
    val fileSizeUnitIndex = min(
        floor(log(this.toDouble(), 1024.0)).toInt(),
        possibleFileSizeUnits.size - 1
    )
    val result = this.toDouble() / 1024.0.pow(fileSizeUnitIndex)
    return "${DecimalFormat("0.##").format(result)} ${possibleFileSizeUnits[fileSizeUnitIndex]}"
}

sealed class ApplicationOutput(val marker: String)

object NotificationOutput : ApplicationOutput(":")
object WarningOutput : ApplicationOutput("!")
object MessageOutput : ApplicationOutput("[MSG]")
object FileOutput : ApplicationOutput("[FILE]")

fun ApplicationOutput.write(text: String, timestamp: OffsetDateTime? = null, author: String? = null) {
    val output = StringBuilder(" ".repeat(4)).apply {
        if (timestamp != null) append("[${timestamp.hour}:${timestamp.minute}:${timestamp.second}] ")
        if (author != null) append("$author ")
        append("$marker> $text")
    }.toString()
    println(output)
}

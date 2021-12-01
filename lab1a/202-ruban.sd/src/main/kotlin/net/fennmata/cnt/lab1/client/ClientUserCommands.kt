package net.fennmata.cnt.lab1.client

import net.fennmata.cnt.lab1.common.ApplicationResponse
import net.fennmata.cnt.lab1.common.FileDownloadRequest
import net.fennmata.cnt.lab1.common.FilePacket
import net.fennmata.cnt.lab1.common.FileUploadRequest
import net.fennmata.cnt.lab1.common.MessageOutput
import net.fennmata.cnt.lab1.common.MessagePacket
import net.fennmata.cnt.lab1.common.MessageSent
import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.WarningOutput
import net.fennmata.cnt.lab1.common.readable
import net.fennmata.cnt.lab1.common.write
import net.fennmata.cnt.lab1.common.writePacketSafely
import java.io.File
import java.time.OffsetDateTime

object QuitClientCommand : ApplicationResponse<ChatClient> {
    override val command = "quit"
    override suspend fun execute(arg: String?) {
        NotificationOutput.write("Stopping the execution ...")
        ChatClient.close()
    }
}

object PrintClientHelpCommand : ApplicationResponse<ChatClient> {
    override val command = "help"
    override suspend fun execute(arg: String?) {
        TODO()
    }
}

object SendMessage : ApplicationResponse<ChatClient> {
    override val command = "send"
    override suspend fun execute(arg: String?) {
        val timestamp = OffsetDateTime.now()
        val message = arg ?: ""
        val messagePacket = MessagePacket(MessageSent, timestamp, ChatClient.username, message)
        ChatClient.socket.writePacketSafely(ChatClient.coroutineScope, messagePacket) {
            WarningOutput.write("The connection to the server was closed [e: ${it.message}].")
            ChatClient.close()
        }
        MessageOutput.write(message, timestamp, ChatClient.username)
    }
}

object UploadFile : ApplicationResponse<ChatClient> {
    override val command = "upload"
    override suspend fun execute(arg: String?) {
        val file = arg?.let { File(arg) }
        if (file == null || !file.exists() || !file.canRead() || file.isDirectory) {
            WarningOutput.write("Please provide a valid file name for upload.")
            return
        }

        val fileName = file.name
        val fileLength = file.length()

        val requestTimestamp = OffsetDateTime.now()
        val uploadRequest = FilePacket(FileUploadRequest, requestTimestamp, ChatClient.username, fileName, fileLength)
        ChatClient.socket.writePacketSafely(ChatClient.coroutineScope, uploadRequest) {
            WarningOutput.write("The connection to the server was closed [e: ${it.message}].")
            ChatClient.close()
        }
        ChatClient.fileTransferQueue += ChatClient.FileUpload("${ChatClient.username}/$fileName")
        NotificationOutput.write("An upload request of $fileName (${fileLength.readable}) was sent to the server.")
    }
}

object DownloadFile : ApplicationResponse<ChatClient> {
    override val command = "download"
    override suspend fun execute(arg: String?) {
        val args = arg?.let { str -> argsRegex.matchEntire(str)?.let { it.groupValues[1] to it.groupValues[2] } }
        if (args == null) {
            WarningOutput.write("Please provide valid file author and/or name for download.")
            return
        }

        val (fileAuthor, fileName) = args

        val requestTimestamp = OffsetDateTime.now()
        val downloadRequest = FilePacket(FileDownloadRequest, requestTimestamp, fileAuthor, fileName, 0)
        ChatClient.socket.writePacketSafely(ChatClient.coroutineScope, downloadRequest) {
            WarningOutput.write("The connection to the server was closed [e: ${it.message}].")
            ChatClient.close()
        }
        ChatClient.fileTransferQueue += ChatClient.FileDownload("$fileAuthor/$fileName")
        NotificationOutput.write("An download request of $fileName by $fileAuthor was sent to the server.")
    }

    private val argsRegex = Regex(""""(.+)" *"(.+)"""")
}

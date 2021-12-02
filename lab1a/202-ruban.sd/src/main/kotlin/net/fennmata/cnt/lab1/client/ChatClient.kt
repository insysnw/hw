package net.fennmata.cnt.lab1.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fennmata.cnt.lab1.common.Application
import net.fennmata.cnt.lab1.common.ConnectionApproved
import net.fennmata.cnt.lab1.common.ConnectionNotification
import net.fennmata.cnt.lab1.common.ConnectionPacket
import net.fennmata.cnt.lab1.common.ConnectionRejected
import net.fennmata.cnt.lab1.common.ConnectionRequest
import net.fennmata.cnt.lab1.common.DisconnectionPacket
import net.fennmata.cnt.lab1.common.FileDownloadApproved
import net.fennmata.cnt.lab1.common.FileDownloadRejected
import net.fennmata.cnt.lab1.common.FileNotification
import net.fennmata.cnt.lab1.common.FileOutput
import net.fennmata.cnt.lab1.common.FilePacket
import net.fennmata.cnt.lab1.common.FileSent
import net.fennmata.cnt.lab1.common.FileTransferPacket
import net.fennmata.cnt.lab1.common.FileTransferResponsePacket
import net.fennmata.cnt.lab1.common.FileUploadApproved
import net.fennmata.cnt.lab1.common.FileUploadRejected
import net.fennmata.cnt.lab1.common.KeepAlivePacket
import net.fennmata.cnt.lab1.common.KeepAlivePing
import net.fennmata.cnt.lab1.common.MessageOutput
import net.fennmata.cnt.lab1.common.MessagePacket
import net.fennmata.cnt.lab1.common.NotificationOutput
import net.fennmata.cnt.lab1.common.WarningOutput
import net.fennmata.cnt.lab1.common.readPacket
import net.fennmata.cnt.lab1.common.readPacketSafely
import net.fennmata.cnt.lab1.common.readable
import net.fennmata.cnt.lab1.common.write
import net.fennmata.cnt.lab1.common.writePacket
import net.fennmata.cnt.lab1.common.writePacketSafely
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.time.OffsetDateTime

object ChatClient : Application<ChatClient>() {

    override val responses = listOf(
        QuitClientCommand,
        PrintClientHelpCommand,
        SendMessage,
        UploadFile,
        DownloadFile
    )

    override fun initialize() {
        NotificationOutput.write("Please enter the server's IPv4 address.")
        val address = readln()
        NotificationOutput.write("Please enter the server's TCP port.")
        val port = readln().toInt()
        NotificationOutput.write("Please enter your username.")
        val requestedUsername = readln()

        if (shouldOldFilesBeDeleted && !fileStorage.deleteRecursively() || !fileStorage.mkdirs())
            throw IllegalStateException("The server could not initialize file storage")

        socket.connect(InetSocketAddress(address, port))
        NotificationOutput.write("TCP connection to the server @ ${socket.remoteSocketAddress} has been successful.")

        val connectionRequest = ConnectionPacket(ConnectionRequest, OffsetDateTime.now(), requestedUsername)
        socket.writePacket(connectionRequest)
        NotificationOutput.write("A connection request has been sent to the server.")

        val serverAnswer = socket.readPacket()
        check(serverAnswer is ConnectionPacket) { "The server has replied incorrectly" }
        check(serverAnswer.state !is ConnectionRejected) { "The connection request was rejected by the server" }
        check(serverAnswer.state is ConnectionApproved) { "The server has replied incorrectly" }

        username = serverAnswer.clientName
        NotificationOutput.write("You have been connected to the chat as $username.", serverAnswer.timestamp)
    }

    override suspend fun execute() {
        coroutineScope.launch(Dispatchers.Default) {
            while (isRunning && isKeptAlive) {
                val keepAlivePacket = KeepAlivePacket(KeepAlivePing, OffsetDateTime.now())
                socket.writePacketSafely(coroutineScope, keepAlivePacket) {} ?: continue
                delay(1000L)
            }
        }

        while (isRunning) {
            val packet = socket.readPacketSafely(coroutineScope) {
                WarningOutput.write("The connection to the server was closed [e: ${it.message}].")
                close()
            } ?: continue

            when (packet) {
                is ConnectionPacket -> process(packet)
                is DisconnectionPacket -> process(packet)
                is MessagePacket -> process(packet)
                is FilePacket -> Unit
                is FileTransferPacket -> process(packet)
                is FileTransferResponsePacket -> process(packet)
                is KeepAlivePacket -> Unit
            }
        }
    }

    override fun finalize() {
        socket.close()
    }

    val socket = Socket()

    var isKeptAlive = true

    var shouldOldFilesBeDeleted = true

    lateinit var username: String

    val fileTransferQueue = mutableListOf<FileTransfer>()

    private val fileStorage = File("files")

    private fun process(packet: ConnectionPacket) = with(packet) {
        if (state !is ConnectionNotification) return@with
        NotificationOutput.write("User $clientName has connected to the chat.", timestamp)
    }

    private fun process(packet: DisconnectionPacket) = with(packet) {
        NotificationOutput.write("User $clientName has disconnected from the chat.", timestamp)
    }

    private fun process(packet: MessagePacket) = with(packet) {
        MessageOutput.write(message, timestamp, clientName)
    }

    private fun process(packet: FileTransferPacket) = with(packet) {
        if (state !is FileNotification) return@with
        FileOutput.write("[$fileName] (${fileLength.readable})", timestamp, clientName)
    }

    private suspend fun process(packet: FileTransferResponsePacket) = with(packet) {
        when (state) {
            is FileUploadRejected -> {
                val rejectedTransfer = fileTransferQueue.find { it is FileUpload && it.fullFileName == fileName }
                    ?: return@with
                fileTransferQueue.remove(rejectedTransfer)
                WarningOutput.write("The server has rejected the upload request for $fileName.")
            }
            is FileDownloadRejected -> {
                val rejectedTransfer = fileTransferQueue.find { it is FileDownload && it.fullFileName == fileName }
                    ?: return@with
                fileTransferQueue.remove(rejectedTransfer)
                WarningOutput.write("The server has rejected the download request for $fileName.")
            }
            is FileUploadApproved -> {
                val approvedTransfer = fileTransferQueue.find { it is FileUpload && it.fullFileName == fileName }
                    ?: return@with
                fileTransferQueue.remove(approvedTransfer)

                approvedTransfer as FileUpload
                transferFile(fileName, InetSocketAddress(socket.inetAddress.hostAddress, socketPort)) {
                    val targetFile = approvedTransfer.file
                    if (!targetFile.exists()) {
                        WarningOutput.write("The file for upload was deleted during operation. Aborting transfer ...")
                        return@transferFile
                    }
                    val filePacket = FilePacket(
                        FileSent, OffsetDateTime.now(), username, targetFile.readBytes()
                    )
                    writePacketSafely(coroutineScope, filePacket) { e ->
                        WarningOutput.write("The file transfer of $fileName was interrupted [e: ${e.message}].")
                    }
                }
            }
            is FileDownloadApproved -> {
                val approvedTransfer = fileTransferQueue.find { it is FileDownload && it.fullFileName == fileName }
                    ?: return@with
                fileTransferQueue.remove(approvedTransfer)

                approvedTransfer as FileDownload
                transferFile(fileName, InetSocketAddress(socket.inetAddress.hostAddress, socketPort)) {
                    val targetFile = File(fileStorage, approvedTransfer.fileName)
                    if (targetFile.exists() && !targetFile.canWrite()) {
                        WarningOutput.write("Can't save the file on a local machine. Aborting transfer ...")
                        return@transferFile
                    }
                    val filePacket = readPacketSafely(coroutineScope) { e ->
                        WarningOutput.write("The file transfer of $fileName was interrupted [e: ${e.message}].")
                    } ?: return@transferFile
                    if (filePacket !is FilePacket) {
                        WarningOutput.write("The client has replied incorrectly to initiated file transfer.")
                        return@transferFile
                    }
                    targetFile.writeBytes(filePacket.fileContents)
                    NotificationOutput.write("The download of $fileName was successful.")
                }
            }
        }
    }

    private suspend fun transferFile(
        fullFileName: String,
        inetAddress: InetSocketAddress,
        doTransfer: suspend Socket.() -> Unit
    ) {
        coroutineScope.launch(Dispatchers.Default) {
            val fileTransferSocket = Socket()
            val openDeferred = coroutineScope.async(Dispatchers.IO) {
                try {
                    fileTransferSocket.connect(inetAddress)
                } catch (e: SocketException) {
                    WarningOutput.write("$fullFileName transfer connection failed [e: ${e.message}].")
                    null
                }
            }
            openDeferred.await() ?: return@launch

            fileTransferSocket.use { it.doTransfer() }
        }
    }

    sealed interface FileTransfer {
        val authorName: String
        val fileName: String
        val fullFileName: String get() = "$authorName/$fileName"
    }

    data class FileUpload(val file: File) : FileTransfer {
        override val authorName = username
        override val fileName: String = file.name
    }

    data class FileDownload(override val authorName: String, override val fileName: String) : FileTransfer

}

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.*


fun ByteArray.toInt(off: Int = 0, len: Int = 4): Int = ByteBuffer.wrap(this, off, len).int

data class TimeStamp(val s: Int, val ms: Int) {
    companion object {

        fun toShortNtpFormat(byteArray: ByteArray, off: Int): TimeStamp {
            fun toInt(off1: Int) = ((byteArray[off1].toInt() and 0xff) shl 8) or (byteArray[off1 + 1].toInt() and 0xff)
            val s: Int = toInt(off)
            val ms: Int = toInt(off + 2)
            return TimeStamp(s, ms)
        }

        fun toNtpFormat(byteArray: ByteArray, off: Int): TimeStamp =
            byteArray.toTimeStamp(off, 4)

        private fun ByteArray.toTimeStamp(off: Int, len: Int): TimeStamp {
            val s = toInt(off, len)
            val ms = toInt(off + len, len)
            return TimeStamp(s, ms)
        }
    }
}

class NtpProtocol(
    val header: Int,
    val rootDelay: TimeStamp,
    val rootDispersion: TimeStamp,
    val referenceId: Int,
    val referenceTimestamp: TimeStamp,
    val originTimeStamp: TimeStamp,
    val receiveTimeStamp: TimeStamp,
    val transmitTimeStamp: TimeStamp,
) {
    companion object {
        fun parseDatagram(byteArray: ByteArray): NtpProtocol =
            NtpProtocol(
                byteArray.toInt(),
                rootDelay = TimeStamp.toShortNtpFormat(byteArray, 4),
                rootDispersion = TimeStamp.toShortNtpFormat(byteArray, 8),
                referenceId = byteArray.toInt(12),
                referenceTimestamp = TimeStamp.toNtpFormat(byteArray, 16),
                originTimeStamp = TimeStamp.toNtpFormat(byteArray, 24),
                receiveTimeStamp = TimeStamp.toNtpFormat(byteArray, 32),
                transmitTimeStamp = TimeStamp.toNtpFormat(byteArray, 40)
            )

        fun unixToNTP(ms: Long) = ms + 2208988800000

        fun responseDatagram(sent: NtpProtocol): ByteArray {
            // 00 100 100 00000000 00000000 00000000
            val time = unixToNTP(Date().time)
            val ms: Int = (time % 1000).toInt()
            val s: Int = (time / 1000).toInt()
            return ByteBuffer.allocate(48)
                // 00 100 100 00000000 00000000 00000000
                .putInt(603979776)
                .putInt(0)
                .putInt(0)
                .putInt(0)
                .putInt(0)
                .putInt(sent.referenceTimestamp.ms).putInt(sent.referenceTimestamp.s)
                .putInt(ms).putInt(s)
                .putInt(ms).putInt(s).array()
        }
    }
}


class NtpServer(val address: InetSocketAddress) {

    fun start() {
        val serverSocket = DatagramSocket(address)
        val receivingDataBuffer = ByteArray(48)
        val inputPacket = DatagramPacket(receivingDataBuffer, receivingDataBuffer.size)

        while (true) {
            serverSocket.receive(inputPacket)
            println("Connect")
            processing(serverSocket, inputPacket)
        }
    }
}

fun processing(serverSocket: DatagramSocket, inputPacket: DatagramPacket) {
    val receivedData = NtpProtocol.parseDatagram(inputPacket.data)
    println("Получено байт: ${inputPacket.length}")

    val senderAddress = inputPacket.address
    val senderPort = inputPacket.port

    val sendingDataBuffer = NtpProtocol.responseDatagram(receivedData)

    val outputPacket = DatagramPacket(
        sendingDataBuffer, sendingDataBuffer.size,
        senderAddress, senderPort
    )
    serverSocket.send(outputPacket)


}


fun main() {
    val address = InetSocketAddress("localhost", 8889)
    NtpServer(address).start()
}
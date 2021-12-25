import java.io.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

enum class PacketType(val opCode: Byte) {
    Read(1),
    Write(2),
    Data(3),
    Acknowledgment(4),
    Error(5);

    companion object {
        fun toPacketType(byte: Byte) = values().find { it.opCode == byte } ?: throw IllegalArgumentException()
    }
}

sealed class TFTPPacket(val opCode: Byte) {
    class RW(opCode: PacketType, fn: String, mode: String = "octet") : TFTPPacket(opCode.opCode) {
        val byteArray: ByteArray

        init {
            byteArray = ByteArray(2 + fn.length + 1 + mode.length + 1)
            fun stringToBytes(off: Int, str: String) {
                for (i in off until off + str.length)
                    byteArray[i] = str[i - off].code.toByte()
            }

            byteArray[1] = opCode.opCode
            stringToBytes(2, fn)
            stringToBytes(2 + fn.length + 1, mode)
        }
    }

    class Data(val block: Int, val data: ByteArray) : TFTPPacket(PacketType.Data.opCode)
    class Acknowledgment(blockInBytes: ByteArray) : TFTPPacket(PacketType.Acknowledgment.opCode) {
        val byteArray: ByteArray
        val block: Int = blockInBytes.toInt(0)

        init {
            byteArray = byteArrayOf(0, PacketType.Acknowledgment.opCode, blockInBytes[0], blockInBytes[1])
        }
    }

    class Error(val errCode: String, val errMessage: String) : TFTPPacket(PacketType.Error.opCode) {
        companion object {
            fun parseError(byteArray: ByteArray): Error {
                val errCode = String(byteArray, 3, 1)
                val errMessage = String(byteArray, 4, byteArray.size - 4)
                return Error(errCode, errMessage)
            }
        }

        override fun toString(): String {
            return "Error(errCode=$errCode, errMessage='$errMessage')"
        }

    }
}

fun ByteArray.toInt(off: Int) = ((this[off].toInt() and 0xff) shl 8) or (this[off + 1].toInt() and 0xff)

class TFTPClient(private val address: InetSocketAddress) {
    fun putFile(fn: String): Unit = TODO()

    private fun getFile(fn1: String, fn2: String, socket: DatagramSocket) {
        val packet = TFTPPacket.RW(PacketType.Read, fn1)
        val readDatagramPacket = DatagramPacket(packet.byteArray, packet.byteArray.size, address)
        socket.send(readDatagramPacket)

        val outputStream1 = receiveFile(socket)
        try {
            val outputStream: OutputStream = FileOutputStream(fn2)
            outputStream1.writeTo(outputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun sendAcknowledgment(block: ByteArray, socket: DatagramSocket, port: Int) {
        val acknowledgment = TFTPPacket.Acknowledgment(block)

        // Подтверждение получения на порт который вернул сервер
        val ack = DatagramPacket(acknowledgment.byteArray, acknowledgment.byteArray.size, address.address, port)
        try {
            socket.send(ack)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun receiveFile(socket: DatagramSocket): ByteArrayOutputStream {
        var prevBlock: ByteArray? = null
        val byteOutOS = ByteArrayOutputStream()
        var block = 1
        do {
            val inputByteArray = ByteArray(packetSize)
            val inputPacket = DatagramPacket(inputByteArray,
                inputByteArray.size, address)

            socket.receive(inputPacket)

            when (PacketType.toPacketType(inputByteArray[1])) {
                PacketType.Data -> {
                    val blockNumber = byteArrayOf(inputByteArray[2], inputByteArray[3])

                    if (!blockNumber.contentEquals(prevBlock)) {
                        val dos = DataOutputStream(byteOutOS)
                        dos.write(inputPacket.data, 4,
                            inputPacket.length - 4)
                        prevBlock = blockNumber.clone()

                        println("Package number: ${block++}")
                    }
                    sendAcknowledgment(blockNumber, socket, inputPacket.port)
                }
                PacketType.Error -> {
                    val e = TFTPPacket.Error.parseError(inputByteArray)
                    println(e)
                }
                else -> throw IllegalArgumentException()
            }
        } while (inputPacket.length == packetSize)

        return byteOutOS
    }

    fun client(fn1: String, fn2: String, type: PacketType) {
        val socket = DatagramSocket()

        when (type) {
            PacketType.Read -> getFile(fn1, fn2, socket)
            PacketType.Write -> putFile(fn1)
            else -> Unit
        }

        socket.close()
    }

}

const val serverIp = "127.0.0.1"
const val serverPort = 69
const val packetSize = 516

fun main() {
    val fn1 = "/srv/tftp/video"
    val fn2 = "video1"

    val address = InetSocketAddress(serverIp, serverPort)
    TFTPClient(address).client(fn1, fn2, PacketType.Read)
}
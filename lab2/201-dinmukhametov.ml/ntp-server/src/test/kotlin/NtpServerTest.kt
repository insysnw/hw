import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class NtpServerTest {

    @Test
    fun toIntConversion() {
        val num = 12233
        val byteArray = ByteBuffer.allocate(8).putInt(num).putInt(11020).array()
        val buf = ByteBuffer.allocate(4).putInt(66000).array()
        val a = ((buf[2].toInt() and 0xff) shl 8) or (buf[3].toInt() and 0xff)
        assertEquals(464, a)
        assertEquals(num, byteArray.toInt(0, 4))
        assertEquals(11020, byteArray.toInt(4, 4))

        val b = ByteBuffer.allocate(4).putInt(44).array()
        assertEquals(0, TimeStamp.toShortNtpFormat(b, 0).s)

    }

}
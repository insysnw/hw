package net.fennmata.cnt.lab1.common

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.api.Assertions.assertEquals

class CommonUtilTests {

    @ParameterizedTest
    @ValueSource(longs = [0, 1, 4, 5, 12, 48, 256, 345, 435345, 334253453, 234532453245, 345345324534563415])
    fun longToBytesTest(input: Long) {
        val output = input.toByteArray(8)
        assertEquals(input, output.toLongWithoutSign())
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 4, 5, 12, 48, 256, 345, 435345, 334253453])
    fun intToBytesTest(input: Int) {
        val output = input.toByteArray(4)
        assertEquals(input, output.toIntWithoutSign())
    }

}

package net.fennmata.cnt.lab1.common

import java.nio.ByteBuffer

class PacketBuffer(vararg outerParameters: Pair<Any, Int>) {

    enum class State { AT_HEADER, AT_BODY_CONSTS, AT_BODY_VARS, PACKET_COMPLETE, PACKET_INCORRECT }

    var state = State.AT_HEADER
        private set

    fun put(bytes: ByteArray) {
        check(state !in finishStates) {
            "a packet buffer can't be updated after completing a packet or determining it was incorrect"
        }
        for ((index, byte) in bytes.withIndex()) {
            if (state in finishStates) {
                bufferOverflow = bytes.sliceArray(index until bytes.size)
                break
            }
            put(byte)
        }
    }

    private fun put(byte: Byte) {
        when (state) {
            State.AT_HEADER -> {
                headerBuffer.put(byte)
                checkStateAtHeader()
            }
            State.AT_BODY_CONSTS -> {
                bodyConstsBuffer.put(byte)
                checkStateAtBodyConsts()
            }
            State.AT_BODY_VARS -> {
                bodyVarsBuffer.put(byte)
                checkStateAtBodyVars()
            }
            else -> throw IllegalStateException("a supposedly unreachable code point was reached")
        }
    }

    private fun checkStateAtHeader() {
        when (headerBuffer.remaining()) {
            1 -> {
                val protocolVersion = headerBuffer.get(0).toInt()
                if (protocolVersion != 1) state = State.PACKET_INCORRECT
            }
            0 -> {
                val supposedEvent = getPacketEventByNumber(headerBuffer.get(1).toInt())
                if (supposedEvent == null) {
                    state = State.PACKET_INCORRECT
                    return
                }
                event = supposedEvent
                bodyConstsBuffer = ByteBuffer.allocate(event.bodyConstsWidth)
                bodyVarsWidth += event.getAdditionalBodyVarsWidth(info)
                if (event.bodyConstsWidth != 0) {
                    state = State.AT_BODY_CONSTS
                } else {
                    bodyVarsBuffer = ByteBuffer.allocate(bodyVarsWidth)
                    state = if (bodyVarsWidth != 0) State.AT_BODY_VARS else State.PACKET_COMPLETE
                }
            }
        }
    }

    private fun checkStateAtBodyConsts() {
        if (bodyConstsBuffer.remaining() == 0) {
            val widthStorageFields = event.fields.filterIsInstance<WidthStorageField>()
            for (field in widthStorageFields) {
                val range = event.getRangeOf(field)
                bodyVarsWidth += bodyConsts.sliceArray(range).toIntWithoutSign()
            }
            bodyVarsBuffer = ByteBuffer.allocate(bodyVarsWidth)
            state = if (bodyVarsWidth != 0) State.AT_BODY_VARS else State.PACKET_COMPLETE
        }
    }

    private fun checkStateAtBodyVars() {
        if (bodyVarsBuffer.remaining() == 0) state = State.PACKET_COMPLETE
    }

    fun toPacket(): Packet {
        check(state == State.PACKET_COMPLETE) {
            "a packet buffer can't return a packet without completing it first"
        }
        return buildPacket(event, bodyConsts + bodyVars)
    }

    val leftoverBytes: ByteArray get() {
        check(state in finishStates) {
            "a packet buffer can't return leftover bytes without completing a packet or determining it was incorrect"
        }
        return bufferOverflow
    }

    private val info = outerParameters.toMap()

    private val headerBuffer = ByteBuffer.allocate(2)

    private lateinit var bodyConstsBuffer: ByteBuffer
    private val bodyConsts by lazy { bodyConstsBuffer.array() }

    private lateinit var bodyVarsBuffer: ByteBuffer
    private val bodyVars by lazy { bodyVarsBuffer.array() }

    private lateinit var event: PacketEvent
    private var bodyVarsWidth = 0

    private var bufferOverflow: ByteArray = byteArrayOf()

    private companion object {
        private val finishStates = setOf(State.PACKET_COMPLETE, State.PACKET_INCORRECT)
    }

}

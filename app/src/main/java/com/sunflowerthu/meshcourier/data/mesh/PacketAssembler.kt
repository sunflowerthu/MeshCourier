package com.sunflowerthu.meshcourier.data.mesh

import java.nio.ByteBuffer

class PacketAssembler {

    private var expectedLength: Int = -1
    private var buffer: ByteBuffer? = null

    fun append(chunk: ByteArray): ByteArray? {
        val bb = ByteBuffer.wrap(chunk)

        if (expectedLength < 0) {
            expectedLength = bb.short.toInt() and 0xFFFF
            buffer = ByteBuffer.allocate(expectedLength)
        }

        val target = buffer ?: return null
        val remaining = target.remaining()
        val toCopy = minOf(bb.remaining(), remaining)

        val temp = ByteArray(toCopy)
        bb.get(temp)
        target.put(temp)

        return if (!target.hasRemaining()) {
            val result = target.array()
            reset()
            result
        } else null
    }

    private fun reset() {
        expectedLength = -1
        buffer = null
    }
}
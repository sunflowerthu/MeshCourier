package com.sunflowerthu.meshcourier.domain.models

import org.junit.Assert.*
import org.junit.Test

class NodeIdTest {

    @Test
    fun `valid string creates NodeId`() {
        val id = NodeId("abc")
        assertEquals("abc", id.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank string throws`() {
        NodeId("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty string throws`() {
        NodeId("")
    }

    @Test
    fun `BROADCAST constant has expected value`() {
        assertEquals("__broadcast__", NodeId.BROADCAST.value)
    }

    @Test
    fun `two NodeIds with same value are equal`() {
        assertEquals(NodeId("x"), NodeId("x"))
    }

    @Test
    fun `two NodeIds with different values are not equal`() {
        assertNotEquals(NodeId("a"), NodeId("b"))
    }
}

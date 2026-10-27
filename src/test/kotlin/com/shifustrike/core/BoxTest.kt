package com.shifustrike.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoxTest {
    @Test
    fun `caixa normaliza cantos e inclui o bloco final`() {
        val b = Box.of(5, 0, 5, 0, 3, 0)
        assertEquals(Box(0, 0, 0, 5, 3, 5), b)
        assertTrue(b.contains(5.9, 3.5, 0.0))
        assertTrue(b.contains(0.0, 0.0, 0.0))
        assertFalse(b.contains(6.0, 0.0, 0.0))
        assertFalse(b.contains(-0.1, 1.0, 1.0))
        assertFalse(b.contains(1.0, 4.0, 1.0))
    }
}

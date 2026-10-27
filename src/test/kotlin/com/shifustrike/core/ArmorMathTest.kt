package com.shifustrike.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ArmorMathTest {
    @Test
    fun `colete corta metade no corpo`() =
        assertEquals(ArmorResult(50f, 75), ArmorMath.apply(100f, 100, helmet = false, headshot = false))

    @Test
    fun `sem capacete headshot ignora colete`() =
        assertEquals(ArmorResult(100f, 100), ArmorMath.apply(100f, 100, helmet = false, headshot = true))

    @Test
    fun `com capacete headshot usa colete`() =
        assertEquals(ArmorResult(50f, 75), ArmorMath.apply(100f, 100, helmet = true, headshot = true))

    @Test
    fun `colete acaba no meio do tiro`() =
        assertEquals(ArmorResult(80f, 0), ArmorMath.apply(100f, 10, helmet = false, headshot = false))

    @Test
    fun `sem colete nada muda`() =
        assertEquals(ArmorResult(40f, 0), ArmorMath.apply(40f, 0, helmet = true, headshot = false))
}

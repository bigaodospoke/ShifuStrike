package com.shifustrike.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoundClockTest {
    private fun relogio() = RoundClock(RoundTimings()).also { it.startRound() }
    private fun RoundClock.avanca(n: Int) = repeat(n) { tick() }

    @Test
    fun `congela 10s e depois libera`() {
        val c = relogio()
        c.avanca(199)
        assertEquals(Phase.FREEZE, c.phase)
        assertTrue(c.frozen)
        c.avanca(1)
        assertEquals(Phase.LIVE, c.phase)
        assertFalse(c.frozen)
    }

    @Test
    fun `compra aberta ate 30s`() {
        val c = relogio()
        c.avanca(599)
        assertTrue(c.buyOpen)
        c.avanca(1)
        assertFalse(c.buyOpen)
    }

    @Test
    fun `rodada de 1min55 conta depois do congelamento`() {
        val c = relogio()
        c.avanca(200 + 2300 - 1)
        assertFalse(c.roundTimeOver)
        assertEquals(1, c.roundTimeLeft)
        c.avanca(1)
        assertTrue(c.roundTimeOver)
    }

    @Test
    fun `plantar para o relogio da rodada`() {
        val c = relogio()
        c.avanca(1000)
        c.plantBomb()
        val restante = c.roundTimeLeft
        c.avanca(3000)
        assertEquals(Phase.PLANTED, c.phase)
        assertEquals(restante, c.roundTimeLeft)
        assertFalse(c.roundTimeOver)
    }

    @Test
    fun `bomba explode 40s depois`() {
        val c = relogio()
        c.avanca(300)
        c.plantBomb()
        c.avanca(799)
        assertFalse(c.bombExploded)
        assertEquals(1, c.bombTimeLeft)
        c.avanca(1)
        assertTrue(c.bombExploded)
    }

    @Test
    fun `fim de rodada dura 5s`() {
        val c = relogio()
        c.avanca(300)
        c.endRound()
        assertEquals(Phase.ROUND_END, c.phase)
        assertFalse(c.buyOpen)
        c.avanca(99)
        assertFalse(c.roundEndDone)
        c.avanca(1)
        assertTrue(c.roundEndDone)
    }
}

package com.shifustrike.core

import kotlin.test.Test
import kotlin.test.assertEquals

class EconomyTest {
    private val r = EconomyRules()

    @Test
    fun `derrotas seguidas sobem 500 ate 3400`() =
        assertEquals(listOf(1400, 1900, 2400, 2900, 3400, 3400), (1..6).map { Economy.lossReward(r, it) })

    @Test
    fun `vitoria por bomba paga 3500 e por eliminacao 3250`() {
        assertEquals(3500, Economy.winReward(r, WinReason.BOMB_EXPLODED))
        assertEquals(3500, Economy.winReward(r, WinReason.BOMB_DEFUSED))
        assertEquals(3250, Economy.winReward(r, WinReason.ELIMINATION))
        assertEquals(3250, Economy.winReward(r, WinReason.TIME))
        assertEquals(3250, Economy.winReward(r, WinReason.FORFEIT))
    }

    @Test
    fun `TR que perde depois de plantar ganha 800 a mais`() {
        val pago = Economy.roundPayout(r, RoundOutcome(Team.CT, WinReason.BOMB_DEFUSED), 1, true)
        assertEquals(1400 + 800, pago[Team.TR])
        assertEquals(3500, pago[Team.CT])
    }

    @Test
    fun `CT que perde nao ganha bonus de plantio`() {
        val pago = Economy.roundPayout(r, RoundOutcome(Team.TR, WinReason.BOMB_EXPLODED), 2, true)
        assertEquals(1900, pago[Team.CT])
        assertEquals(3500, pago[Team.TR])
    }

    @Test
    fun `TR que perde sem plantar ganha so a derrota`() =
        assertEquals(1400, Economy.roundPayout(r, RoundOutcome(Team.CT, WinReason.TIME), 1, false)[Team.TR])

    @Test
    fun `limite de 16000`() {
        assertEquals(16000, Economy.add(r, 15900, 300))
        assertEquals(0, Economy.add(r, 100, -300))
    }

    @Test
    fun `kill por categoria`() {
        assertEquals(100, Economy.killReward(r, KillCategory.AWP))
        assertEquals(1500, Economy.killReward(r, KillCategory.FACA))
        assertEquals(600, Economy.killReward(r, KillCategory.SMG))
        assertEquals(900, Economy.killReward(r, KillCategory.ESCOPETA))
        assertEquals(300, Economy.killReward(r, KillCategory.PADRAO))
    }

    @Test
    fun `dinheiro inicial 500`() = assertEquals(500, r.startMoney)
}

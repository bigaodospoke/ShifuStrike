package com.shifustrike.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoundRulesTest {
    private fun foto(
        ctVivos: Int = 2, trVivos: Int = 2, ctInicio: Int = 2, trInicio: Int = 2,
        bomba: BombState = BombState.NONE, tempoAcabou: Boolean = false,
    ) = RoundSnapshot(ctVivos, trVivos, ctInicio, trInicio, bomba, tempoAcabou)

    @Test
    fun `bomba explodiu ganha TR`() =
        assertEquals(RoundOutcome(Team.TR, WinReason.BOMB_EXPLODED), RoundRules.evaluate(foto(ctVivos = 0, bomba = BombState.EXPLODED)))

    @Test
    fun `bomba desarmada ganha CT`() =
        assertEquals(RoundOutcome(Team.CT, WinReason.BOMB_DEFUSED), RoundRules.evaluate(foto(trVivos = 0, bomba = BombState.DEFUSED)))

    @Test
    fun `CT todos mortos ganha TR mesmo com bomba plantada`() =
        assertEquals(RoundOutcome(Team.TR, WinReason.ELIMINATION), RoundRules.evaluate(foto(ctVivos = 0, bomba = BombState.PLANTED)))

    @Test
    fun `TR todos mortos sem bomba ganha CT`() =
        assertEquals(RoundOutcome(Team.CT, WinReason.ELIMINATION), RoundRules.evaluate(foto(trVivos = 0)))

    @Test
    fun `TR todos mortos com bomba plantada continua`() =
        assertNull(RoundRules.evaluate(foto(trVivos = 0, bomba = BombState.PLANTED)))

    @Test
    fun `tempo acabou sem bomba ganha CT por TIME`() =
        assertEquals(RoundOutcome(Team.CT, WinReason.TIME), RoundRules.evaluate(foto(tempoAcabou = true)))

    @Test
    fun `rodada em andamento nao tem vencedor`() = assertNull(RoundRules.evaluate(foto()))

    @Test
    fun `time vazio desde o inicio nao perde por eliminacao`() =
        assertNull(RoundRules.evaluate(foto(ctVivos = 0, ctInicio = 0, trVivos = 1, trInicio = 1)))

    @Test
    fun `primeiro a 5 vence`() {
        assertEquals(Team.CT, RoundRules.matchWinner(5, 3, 5))
        assertEquals(Team.TR, RoundRules.matchWinner(2, 5, 5))
        assertNull(RoundRules.matchWinner(4, 4, 5))
    }

    @Test
    fun `time oposto`() {
        assertEquals(Team.TR, Team.CT.other())
        assertEquals(Team.CT, Team.TR.other())
    }
}

package com.shifustrike.core

/** Categoria da arma que fez a kill, para a recompensa do CS. */
enum class KillCategory { PADRAO, SMG, ESCOPETA, AWP, FACA }

/** Valores do CS, exceto o dinheiro inicial ($500, pedido do dono). */
data class EconomyRules(
    val startMoney: Int = 500,
    val maxMoney: Int = 16000,
    val winElimination: Int = 3250,
    val winBomb: Int = 3500,
    val lossBase: Int = 1400,
    val lossStep: Int = 500,
    val lossMax: Int = 3400,
    val plantBonus: Int = 800,
    val killRewards: Map<KillCategory, Int> = mapOf(
        KillCategory.PADRAO to 300,
        KillCategory.SMG to 600,
        KillCategory.ESCOPETA to 900,
        KillCategory.AWP to 100,
        KillCategory.FACA to 1500,
    ),
)

object Economy {
    fun winReward(r: EconomyRules, reason: WinReason): Int = when (reason) {
        WinReason.BOMB_EXPLODED, WinReason.BOMB_DEFUSED -> r.winBomb
        WinReason.ELIMINATION, WinReason.TIME, WinReason.FORFEIT -> r.winElimination
    }

    /** `consecutiveLosses` já conta a derrota desta rodada (1 = primeira derrota seguida). */
    fun lossReward(r: EconomyRules, consecutiveLosses: Int): Int =
        minOf(r.lossBase + r.lossStep * (consecutiveLosses.coerceAtLeast(1) - 1), r.lossMax)

    /** Quanto cada jogador de cada time ganha no fim da rodada. */
    fun roundPayout(r: EconomyRules, outcome: RoundOutcome, loserStreak: Int, bombWasPlanted: Boolean): Map<Team, Int> {
        val loser = outcome.winner.other()
        val loss = lossReward(r, loserStreak) + if (loser == Team.TR && bombWasPlanted) r.plantBonus else 0
        return mapOf(outcome.winner to winReward(r, outcome.reason), loser to loss)
    }

    fun killReward(r: EconomyRules, c: KillCategory): Int = r.killRewards[c] ?: 0

    fun add(r: EconomyRules, money: Int, delta: Int): Int = (money + delta).coerceIn(0, r.maxMoney)
}

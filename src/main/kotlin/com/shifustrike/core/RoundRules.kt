package com.shifustrike.core

enum class BombState { NONE, PLANTED, EXPLODED, DEFUSED }

enum class WinReason { ELIMINATION, TIME, BOMB_EXPLODED, BOMB_DEFUSED, FORFEIT }

data class RoundOutcome(val winner: Team, val reason: WinReason)

/** Quantos estão vivos agora e quantos começaram a rodada em cada time, mais o estado da bomba. */
data class RoundSnapshot(
    val ctAlive: Int,
    val trAlive: Int,
    val ctStarted: Int,
    val trStarted: Int,
    val bomb: BombState,
    val timeOver: Boolean,
)

object RoundRules {
    /** Quem ganhou a rodada, ou null se ela continua. A ordem das regras é a da spec (seção 7). */
    fun evaluate(s: RoundSnapshot): RoundOutcome? = when {
        s.bomb == BombState.EXPLODED -> RoundOutcome(Team.TR, WinReason.BOMB_EXPLODED)
        s.bomb == BombState.DEFUSED -> RoundOutcome(Team.CT, WinReason.BOMB_DEFUSED)
        s.ctStarted > 0 && s.ctAlive == 0 -> RoundOutcome(Team.TR, WinReason.ELIMINATION)
        s.trStarted > 0 && s.trAlive == 0 && s.bomb == BombState.NONE -> RoundOutcome(Team.CT, WinReason.ELIMINATION)
        s.timeOver && s.bomb == BombState.NONE -> RoundOutcome(Team.CT, WinReason.TIME)
        else -> null
    }

    fun matchWinner(ct: Int, tr: Int, toWin: Int): Team? = when {
        ct >= toWin -> Team.CT
        tr >= toWin -> Team.TR
        else -> null
    }
}

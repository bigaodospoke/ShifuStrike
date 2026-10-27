package com.shifustrike.core

enum class Phase { FREEZE, LIVE, PLANTED, ROUND_END, MATCH_END }

/** Tempos da rodada em ticks (20 por segundo). */
data class RoundTimings(
    val freezeTicks: Int = 200,
    val buyTicks: Int = 600,
    val roundTicks: Int = 2300,
    val bombTicks: Int = 800,
    val roundEndTicks: Int = 100,
)

/**
 * Relógio de uma rodada. `ticks` conta desde o início da rodada; o tempo de 1:55 só começa
 * depois do congelamento e para quando a bomba é plantada.
 */
class RoundClock(private val t: RoundTimings) {
    var phase = Phase.FREEZE
        private set
    var ticks = 0
        private set
    private var plantedAt = -1
    private var endedAt = -1
    private var frozenRoundLeft = t.roundTicks

    fun startRound() {
        phase = Phase.FREEZE
        ticks = 0
        plantedAt = -1
        endedAt = -1
        frozenRoundLeft = t.roundTicks
    }

    fun tick() {
        ticks++
        if (phase == Phase.FREEZE && ticks >= t.freezeTicks) phase = Phase.LIVE
    }

    fun plantBomb() {
        frozenRoundLeft = roundTimeLeft
        plantedAt = ticks
        phase = Phase.PLANTED
    }

    fun endRound() {
        if (phase == Phase.LIVE) frozenRoundLeft = roundTimeLeft
        endedAt = ticks
        phase = Phase.ROUND_END
    }

    fun endMatch() {
        if (phase == Phase.LIVE) frozenRoundLeft = roundTimeLeft
        phase = Phase.MATCH_END
    }

    val buyOpen: Boolean get() = phase != Phase.ROUND_END && phase != Phase.MATCH_END && ticks < t.buyTicks
    val frozen: Boolean get() = phase == Phase.FREEZE

    val roundTimeLeft: Int
        get() = when (phase) {
            Phase.FREEZE -> t.roundTicks
            Phase.LIVE -> (t.roundTicks - (ticks - t.freezeTicks)).coerceAtLeast(0)
            else -> frozenRoundLeft
        }

    val roundTimeOver: Boolean get() = phase == Phase.LIVE && roundTimeLeft == 0

    val bombTimeLeft: Int
        get() = if (plantedAt < 0) t.bombTicks else (t.bombTicks - (ticks - plantedAt)).coerceAtLeast(0)

    val bombExploded: Boolean get() = phase == Phase.PLANTED && bombTimeLeft == 0

    val roundEndDone: Boolean get() = phase == Phase.ROUND_END && ticks - endedAt >= t.roundEndTicks
}

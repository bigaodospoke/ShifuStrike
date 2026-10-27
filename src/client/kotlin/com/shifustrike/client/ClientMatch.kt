package com.shifustrike.client

import com.shifustrike.core.Team
import com.shifustrike.core.WinReason
import com.shifustrike.item.UseHints
import com.shifustrike.match.KillInfo
import com.shifustrike.network.MatchEndPayload
import com.shifustrike.network.MatchInfoPayload
import com.shifustrike.network.MatchStatePayload
import com.shifustrike.network.RoundEndPayload
import com.shifustrike.network.ScoreLine
import net.minecraft.Util
import net.minecraft.world.phys.Vec3

/** O que o cliente sabe da partida em que está. */
object ClientMatch {
    const val COR_CT = 0x8EC5FF
    const val COR_TR = 0xE8B64C

    data class Banner(val title: String, val subtitle: String, val color: Int, val until: Long, val lines: List<ScoreLine> = emptyList())

    var info: MatchInfoPayload? = null
        private set
    var state: MatchStatePayload? = null
        private set
    val feed = ArrayDeque<Pair<KillInfo, Long>>()
    var banner: Banner? = null
        private set

    val active: Boolean get() = info != null && state != null

    fun teamColor(t: Team?) = if (t == Team.CT) COR_CT else COR_TR
    fun teamName(t: Team) = if (t == Team.CT) "Antiterroristas" else "Terroristas"

    fun now(): Long = Util.getMillis()

    fun onInfo(p: MatchInfoPayload) {
        info = p
        UseHints.sites = listOf(p.siteA, p.siteB)
        UseHints.team = p.team
        UseHints.defuseRadius = p.defuseRadius
    }

    fun onState(p: MatchStatePayload) {
        state = p
        UseHints.bombPos = p.bombPos?.let { Vec3.atBottomCenterOf(it) }
    }

    fun onKill(k: KillInfo) {
        feed.addLast(k to now())
        while (feed.size > 5) feed.removeFirst()
    }

    fun onRoundEnd(p: RoundEndPayload) {
        val motivo = when (p.reason) {
            WinReason.ELIMINATION -> "Eliminação"
            WinReason.TIME -> "Tempo esgotado"
            WinReason.BOMB_EXPLODED -> "A bomba explodiu"
            WinReason.BOMB_DEFUSED -> "A bomba foi desarmada"
            WinReason.FORFEIT -> "O outro time ficou sem jogadores"
        }
        banner = Banner("${teamName(p.winner)} venceram", "$motivo — CT ${p.ctScore} × ${p.trScore} TR", teamColor(p.winner), now() + 4000)
    }

    fun onMatchEnd(p: MatchEndPayload) {
        val w = p.winner
        banner = if (w == null) {
            Banner("Partida cancelada", "", 0xFFFFFF, now() + 4000)
        } else {
            Banner("${teamName(w)} venceram a partida!", "CT ${p.ctScore} × ${p.trScore} TR", teamColor(w), now() + 8000, p.lines)
        }
        clear(keepBanner = true)
    }

    fun clear(keepBanner: Boolean = false) {
        info = null
        state = null
        feed.clear()
        if (!keepBanner) banner = null
        UseHints.clear()
    }
}

package com.shifustrike.match

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.GameType

/** Morto assiste pela câmera de um colega vivo (ou de qualquer vivo, se o time todo morreu). Não fica livre pelo mapa. */
object Spectators {
    private fun targets(m: Match, p: ServerPlayer): List<ServerPlayer> {
        val me = m.players[p.uuid] ?: return emptyList()
        val alive = m.online().filter { (pt, sp) -> pt.alive && sp !== p }
        val mates = alive.filter { it.first.team == me.team }
        return (mates.ifEmpty { alive }).map { it.second }
    }

    fun makeSpectator(m: Match, p: ServerPlayer) {
        p.setGameMode(GameType.SPECTATOR)
        targets(m, p).firstOrNull()?.let { p.setCamera(it) }
    }

    /** Refaz a câmera de quem soltou (shift) ou cujo alvo morreu. */
    fun tick(m: Match) {
        for ((pt, p) in m.online()) {
            if (pt.alive) continue
            if (p.gameMode.gameModeForPlayer != GameType.SPECTATOR) p.setGameMode(GameType.SPECTATOR)
            val cam = p.camera
            val ok = cam is ServerPlayer && cam !== p && m.players[cam.uuid]?.alive == true
            if (!ok) targets(m, p).firstOrNull()?.let { p.setCamera(it) }
        }
    }

    fun cycle(m: Match, p: ServerPlayer) {
        val pt = m.players[p.uuid] ?: return
        if (pt.alive) return
        val list = targets(m, p)
        if (list.isEmpty()) return
        val i = list.indexOfFirst { it === p.camera }
        p.setCamera(list[(i + 1) % list.size])
    }
}

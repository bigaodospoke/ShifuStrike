package com.shifustrike.guard

import com.shifustrike.core.Team
import com.shifustrike.match.Match
import com.shifustrike.match.MatchManager
import com.shifustrike.match.Msg
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level

/** A área do mapa durante a partida: blocos protegidos, ninguém de fora entra, vivo não sai (nem para outra dimensão). */
object AreaGuard {
    fun isProtected(level: Level, x: Double, y: Double, z: Double): Boolean {
        if (level.isClientSide) return false
        val m = MatchManager.match ?: return false
        if (m.state != Match.State.RUNNING) return false
        val area = m.arena.area ?: return false
        return level.dimension().location().toString() == m.arena.dimensao && area.contains(x, y, z)
    }

    fun tick(m: Match) {
        val area = m.arena.area ?: return
        val level = m.level
        val saida = m.arena.saida
        for (p in level.players()) {
            if (p.uuid in m.players) continue
            if (saida != null && area.contains(p.x, p.y, p.z) && p.gameMode.gameModeForPlayer != GameType.SPECTATOR) {
                p.teleportTo(level, saida.x, saida.y, saida.z, saida.yaw, saida.pitch)
                Msg.sendError(p, "Há um evento de CS acontecendo nessa área.")
            }
        }
        for ((pt, p) in m.online()) {
            if (!pt.alive) continue
            if (p.level() === level && area.contains(p.x, p.y, p.z)) continue
            val spawns = if (pt.team == Team.CT) m.arena.spawnsCt else m.arena.spawnsTr
            if (spawns.isEmpty()) continue
            m.teleportToSpawn(p, pt.team, spawns.indices.random(m.random))
        }
    }
}

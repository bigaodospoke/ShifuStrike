package com.shifustrike.item

import com.shifustrike.core.Box
import com.shifustrike.core.Team
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

/**
 * O que o cliente sabe da partida para prever o uso da C4/kit (o servidor decide de verdade).
 * Preenchido pela rede do cliente; no servidor fica vazio e não é usado.
 */
object UseHints {
    @Volatile var sites: List<Box> = emptyList()
    @Volatile var bombPos: Vec3? = null
    @Volatile var team: Team? = null
    @Volatile var defuseRadius: Double = 2.0

    fun canPlant(p: Player): Boolean = team == Team.TR && bombPos == null && p.onGround() && sites.any { it.contains(p.x, p.y, p.z) }

    fun canDefuse(p: Player): Boolean {
        val b = bombPos ?: return false
        return team == Team.CT && p.position().distanceTo(b) <= defuseRadius
    }

    fun clear() {
        sites = emptyList()
        bombPos = null
        team = null
    }
}

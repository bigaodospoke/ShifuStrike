package com.shifustrike.match

import com.shifustrike.core.Economy
import com.shifustrike.core.KillCategory
import com.shifustrike.core.ShopEntry
import com.shifustrike.core.Team
import com.shifustrike.registry.ModItems
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack

/** Uma linha do feed de kills. `killer` null = morreu sozinho (queda, bomba). */
data class KillInfo(
    val killer: String?,
    val killerTeam: Team?,
    val victim: String,
    val victimTeam: Team,
    val weapon: String,
    val headshot: Boolean,
)

object Deaths {
    /** Dano fatal num jogador. Devolve false para participantes: a morte do Minecraft não acontece, ele vira espectador. */
    fun onAllowDeath(victim: ServerPlayer, source: DamageSource): Boolean {
        val m = MatchManager.match ?: return true
        val vt = MatchManager.participant(victim) ?: return true
        victim.health = victim.maxHealth
        if (!vt.alive) return false

        val killer = source.entity as? ServerPlayer
        val kt = killer?.let { MatchManager.participant(it) }
        dropOnDeath(m, victim)
        vt.alive = false
        vt.deaths++
        vt.armor = 0
        vt.helmet = false

        val counts = kt != null && killer !== victim && kt.team != vt.team
        if (counts) {
            kt!!.kills++
            kt.money = Economy.add(m.cfg.economy(), kt.money, Economy.killReward(m.cfg.economy(), killCategory(killer!!, source, m.shop)))
        }
        m.onKill(
            KillInfo(
                killer = if (counts) killer!!.gameProfile.name else null,
                killerTeam = if (counts) kt!!.team else null,
                victim = victim.gameProfile.name,
                victimTeam = vt.team,
                weapon = if (counts) weaponName(killer!!, source, m.shop) else "",
                headshot = counts && vt.lastHitHeadshot,
            ),
        )
        vt.lastHitHeadshot = false
        Spectators.makeSpectator(m, victim)
        return false
    }

    /** Granada (dano que não veio da mão do atacante) conta como arma padrão. */
    fun killCategory(killer: ServerPlayer, source: DamageSource, shop: List<ShopEntry>): KillCategory {
        val direct = source.directEntity
        if (direct != null && direct !== killer) return KillCategory.PADRAO
        val held = killer.mainHandItem
        if (held.`is`(ModItems.FACA)) return KillCategory.FACA
        return Loadouts.entryOf(held, shop)?.categoria ?: KillCategory.PADRAO
    }

    fun weaponName(killer: ServerPlayer, source: DamageSource, shop: List<ShopEntry>): String {
        val direct = source.directEntity
        if (direct != null && direct !== killer) return "Granada Explosiva"
        val held = killer.mainHandItem
        if (held.`is`(ModItems.FACA)) return "Faca"
        return Loadouts.entryOf(held, shop)?.nome ?: held.hoverName.string
    }

    /** Larga no chão a melhor arma e a C4; o resto do inventário some. */
    fun dropOnDeath(m: Match, p: ServerPlayer) {
        p.stopUsingItem()
        p.closeContainer()
        p.inventoryMenu.craftSlots.clearContent()
        val inv = p.inventory
        val drops = mutableListOf<ItemStack>()
        Loadouts.bestWeapon(p, m.shop)?.let { drops += inv.removeItemNoUpdate(it) }
        for (i in 0 until inv.containerSize) if (inv.getItem(i).`is`(ModItems.C4)) drops += inv.removeItemNoUpdate(i)
        inv.clearContent()
        p.containerMenu.carried = ItemStack.EMPTY
        val level = p.serverLevel()
        for (stack in drops) {
            val e = ItemEntity(level, p.x, p.y + 0.5, p.z, stack)
            e.setNoPickUpDelay()
            level.addFreshEntity(e)
        }
    }

    /** Saiu do servidor ou da partida no meio da rodada: conta como morto, sem kill para ninguém. */
    fun removeFromRound(m: Match, p: ServerPlayer, pt: Participant) {
        if (!pt.alive) return
        dropOnDeath(m, p)
        pt.alive = false
    }
}

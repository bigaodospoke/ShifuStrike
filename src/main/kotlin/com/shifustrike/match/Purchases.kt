package com.shifustrike.match

import com.shifustrike.core.BuyCheck
import com.shifustrike.core.BuyContext
import com.shifustrike.core.BuyFail
import com.shifustrike.core.BuyRules
import com.shifustrike.core.EntryKind
import com.shifustrike.core.Team
import com.shifustrike.core.forTeam
import com.shifustrike.pointblank.EventItems
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import java.util.UUID

/** Compra e reembolso, sempre validados no servidor. */
object Purchases {
    fun inBuyZone(m: Match, p: ServerPlayer): Boolean {
        val team = m.players[p.uuid]?.team ?: return false
        val spawns = if (team == Team.CT) m.arena.spawnsCt else m.arena.spawnsTr
        val r2 = m.cfg.raioZonaCompra * m.cfg.raioZonaCompra
        return spawns.any { p.distanceToSqr(it.x, it.y, it.z) <= r2 }
    }

    private fun failText(f: BuyFail) = when (f) {
        BuyFail.MORTO -> "Mortos não compram."
        BuyFail.FORA_DO_TEMPO -> "O tempo de compra acabou."
        BuyFail.FORA_DA_ZONA -> "Volte para perto do seu spawn para comprar."
        BuyFail.TIME_ERRADO -> "Esse item é do outro time."
        BuyFail.EM_BREVE -> "Esse item chega numa próxima versão."
        BuyFail.JA_TEM -> "Você já tem esse item."
        BuyFail.LIMITE -> "Você já carrega o máximo desse item."
        BuyFail.SEM_DINHEIRO -> "Dinheiro insuficiente."
    }

    private fun drop(p: ServerPlayer, stack: ItemStack) {
        val e = ItemEntity(p.serverLevel(), p.x, p.y + 0.5, p.z, stack)
        e.setPickUpDelay(40)
        p.serverLevel().addFreshEntity(e)
    }

    fun buy(m: Match, p: ServerPlayer, chave: String): BuyCheck {
        val pt = m.players[p.uuid] ?: return BuyCheck.Fail(BuyFail.MORTO)
        val e = m.shop.forTeam(pt.team).firstOrNull { it.chave == chave }
            ?: return BuyCheck.Fail(if (m.shop.any { it.chave == chave }) BuyFail.TIME_ERRADO else BuyFail.EM_BREVE).also {
                Msg.actionBar(p, failText(it.reason))
            }
        val ctx = BuyContext(pt.team, pt.money, pt.alive && m.state == Match.State.RUNNING, m.clock.buyOpen, inBuyZone(m, p), Loadouts.ownedOf(p, pt, m.shop))
        val check = BuyRules.check(e, ctx)
        if (check !is BuyCheck.Ok) {
            Msg.actionBar(p, failText((check as BuyCheck.Fail).reason))
            return check
        }
        var stackId: UUID? = null
        when (e.tipo) {
            EntryKind.PRIMARIA, EntryKind.PISTOLA, EntryKind.GRANADA -> {
                check.replaces?.let { old ->
                    val inv = p.inventory
                    val slot = (0 until inv.containerSize).firstOrNull { Loadouts.entryOf(inv.getItem(it), m.shop)?.chave == old }
                    slot?.let { drop(p, inv.removeItemNoUpdate(it)) }
                }
                stackId = UUID.randomUUID()
                if (!Loadouts.giveWeapon(p, e, stackId)) return BuyCheck.Fail(BuyFail.EM_BREVE)
            }
            EntryKind.COLETE -> pt.armor = 100
            EntryKind.COLETE_CAPACETE -> {
                pt.armor = 100
                pt.helmet = true
            }
            EntryKind.EM_BREVE -> return BuyCheck.Fail(BuyFail.EM_BREVE)
        }
        pt.money -= check.price
        pt.purchases += Purchase(e.chave, check.price, stackId)
        return check
    }

    /** Desfaz a última compra daquele item nesta rodada, se ainda for tempo de compra e o item ainda estiver com o jogador. */
    fun refund(m: Match, p: ServerPlayer, chave: String): Boolean {
        if (!m.clock.buyOpen || m.state != Match.State.RUNNING) return false
        val pt = m.players[p.uuid] ?: return false
        if (!pt.alive) return false
        val buy = pt.purchases.lastOrNull { it.chave == chave } ?: return false
        val e = m.shop.firstOrNull { it.chave == chave } ?: return false
        when (e.tipo) {
            EntryKind.COLETE -> {
                if (pt.armor < 100) return false
                pt.armor = 0
                pt.helmet = false
            }
            EntryKind.COLETE_CAPACETE -> {
                if (pt.armor < 100) return false
                pt.helmet = false
                if (buy.price != e.precoAlt) pt.armor = 0
            }
            else -> {
                val id = buy.stackId ?: return false
                val inv = p.inventory
                val slots = (0 until inv.containerSize).filter { EventItems.purchaseId(inv.getItem(it)) == id }
                // a arma em si precisa estar com o jogador (não vale reembolsar só a munição)
                if (slots.none { Loadouts.slotOf(inv.getItem(it), m.shop) != null }) return false
                slots.forEach { inv.setItem(it, ItemStack.EMPTY) }
            }
        }
        pt.money += buy.price
        pt.purchases.remove(buy)
        return true
    }
}

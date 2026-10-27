package com.shifustrike.guard

import com.shifustrike.core.Team
import com.shifustrike.match.Loadouts
import com.shifustrike.match.Match
import com.shifustrike.match.MatchManager
import com.shifustrike.pointblank.EventItems
import com.shifustrike.registry.ModItems
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/** Itens do evento ficam na partida: sem baús, sem bater em entidades de fora, regras de pegar do chão. */
object InteractionGuard {
    fun register() {
        UseBlockCallback.EVENT.register { player, level, _, hit ->
            if (level.isClientSide || !MatchManager.isPlaying(player.uuid)) return@register InteractionResult.PASS
            val pos = hit.blockPos
            val container = level.getBlockEntity(pos) is Container || level.getBlockState(pos).getMenuProvider(level, pos) != null
            if (container) InteractionResult.FAIL else InteractionResult.PASS
        }
        AttackEntityCallback.EVENT.register { player, level, _, target, _ ->
            if (!level.isClientSide && MatchManager.isPlaying(player.uuid) && target !is Player) InteractionResult.FAIL else InteractionResult.PASS
        }
    }

    fun allowPickup(p: Player, stack: ItemStack): Boolean {
        val sp = p as? ServerPlayer ?: return true
        val event = EventItems.isEvent(stack)
        val m = MatchManager.match?.takeIf { it.state == Match.State.RUNNING } ?: return !event
        val pt = m.players[sp.uuid] ?: return !event
        if (!pt.alive || !event) return false
        if (stack.`is`(ModItems.C4)) return pt.team == Team.TR
        val slot = Loadouts.slotOf(stack, m.shop)
        if (slot == Loadouts.SLOT_PRIMARIA || slot == Loadouts.SLOT_PISTOLA) {
            val inv = sp.inventory
            return (0 until inv.containerSize).none { Loadouts.slotOf(inv.getItem(it), m.shop) == slot }
        }
        return true
    }
}

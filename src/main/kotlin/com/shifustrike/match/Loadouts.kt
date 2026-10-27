package com.shifustrike.match

import com.shifustrike.core.DefaultShop
import com.shifustrike.core.EntryKind
import com.shifustrike.core.Owned
import com.shifustrike.core.ShopEntry
import com.shifustrike.core.Team
import com.shifustrike.core.forTeam
import com.shifustrike.pointblank.EventItems
import com.shifustrike.pointblank.PointBlankItems
import com.shifustrike.registry.ModItems
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import java.util.UUID

/** Hotbar fixa do CS e entrega de armas. */
object Loadouts {
    const val SLOT_PRIMARIA = 0
    const val SLOT_PISTOLA = 1
    const val SLOT_FACA = 2
    const val SLOT_GRANADA = 3
    /** C4 no TR, kit de desarme no CT. */
    const val SLOT_EXTRA = 4

    private fun itemId(stack: ItemStack): String = BuiltInRegistries.ITEM.getKey(stack.item).toString()

    /** A entrada da loja daquele item (procura em todos os times: arma pega do chão pode ser do outro time). */
    fun entryOf(stack: ItemStack, shop: List<ShopEntry>): ShopEntry? {
        if (stack.isEmpty) return null
        val id = itemId(stack)
        return shop.firstOrNull { it.item == id }
    }

    fun slotOf(stack: ItemStack, shop: List<ShopEntry>): Int? = when {
        stack.isEmpty -> null
        stack.`is`(ModItems.FACA) -> SLOT_FACA
        stack.`is`(ModItems.C4) || stack.`is`(ModItems.KIT) -> SLOT_EXTRA
        else -> when (entryOf(stack, shop)?.tipo) {
            EntryKind.PRIMARIA -> SLOT_PRIMARIA
            EntryKind.PISTOLA -> SLOT_PISTOLA
            EntryKind.GRANADA -> SLOT_GRANADA
            else -> null
        }
    }

    /** Põe na parte de cima do inventário (slots 9-35), longe da hotbar; se lotar, onde couber. */
    fun insertMain(p: ServerPlayer, stack: ItemStack) {
        val inv = p.inventory
        for (i in 9 until 36) {
            if (stack.isEmpty) return
            val cur = inv.getItem(i)
            if (!cur.isEmpty && ItemStack.isSameItemSameComponents(cur, stack) && cur.count < cur.maxStackSize) {
                val n = minOf(stack.count, cur.maxStackSize - cur.count)
                cur.grow(n)
                stack.shrink(n)
            }
        }
        for (i in 9 until 36) {
            if (stack.isEmpty) return
            if (inv.getItem(i).isEmpty) {
                inv.setItem(i, stack.copy())
                stack.count = 0
            }
        }
        if (!stack.isEmpty) inv.add(stack)
    }

    /** Arma (carregada, com reserva) ou granada no slot certo, marcada como item do evento. */
    fun giveWeapon(p: ServerPlayer, e: ShopEntry, purchaseId: UUID?): Boolean {
        val slot = when (e.tipo) {
            EntryKind.PRIMARIA -> SLOT_PRIMARIA
            EntryKind.PISTOLA -> SLOT_PISTOLA
            EntryKind.GRANADA -> SLOT_GRANADA
            else -> return false
        }
        val stack = if (e.tipo == EntryKind.GRANADA) PointBlankItems.grenade() else PointBlankItems.loadedGun(p, e.item!!, e.pente)
        if (stack == null) return false
        p.inventory.setItem(slot, EventItems.mark(stack, purchaseId))
        if (e.municao != null && e.reserva > 0) {
            PointBlankItems.ammo(e.municao, e.reserva).forEach { insertMain(p, EventItems.mark(it)) }
        }
        return true
    }

    /** Início de rodada. `fresh` (morreu ou é a 1ª rodada): limpa e dá a pistola inicial. Sempre garante faca e, no CT, o kit. */
    fun giveRoundBasics(p: ServerPlayer, team: Team, shop: List<ShopEntry>, fresh: Boolean) {
        if (fresh) {
            p.inventory.clearContent()
            shop.forTeam(team).firstOrNull { it.chave == DefaultShop.starterKey(team) }?.let { giveWeapon(p, it, null) }
        }
        if (p.inventory.countItem(ModItems.FACA) == 0) p.inventory.setItem(SLOT_FACA, ItemStack(ModItems.FACA))
        if (team == Team.CT && p.inventory.countItem(ModItems.KIT) == 0) p.inventory.setItem(SLOT_EXTRA, ItemStack(ModItems.KIT))
    }

    /** A arma que cai quando o jogador morre: a primária, ou a pistola se não tiver primária. */
    fun bestWeapon(p: ServerPlayer, shop: List<ShopEntry>): Int? {
        val inv = p.inventory
        val slots = (0 until inv.containerSize).filter { !inv.getItem(it).isEmpty }
        return slots.firstOrNull { slotOf(inv.getItem(it), shop) == SLOT_PRIMARIA }
            ?: slots.firstOrNull { slotOf(inv.getItem(it), shop) == SLOT_PISTOLA }
    }

    fun ownedOf(p: ServerPlayer, part: Participant, shop: List<ShopEntry>): Owned {
        val inv = p.inventory
        val stacks = (0 until inv.containerSize).map { inv.getItem(it) }.filter { !it.isEmpty }
        fun keyOf(kind: EntryKind) = stacks.firstNotNullOfOrNull { s -> entryOf(s, shop)?.takeIf { it.tipo == kind }?.chave }
        val granadas = stacks.filter { entryOf(it, shop)?.tipo == EntryKind.GRANADA }.sumOf { it.count }
        return Owned(keyOf(EntryKind.PRIMARIA), keyOf(EntryKind.PISTOLA), part.armor, part.helmet, granadas)
    }

    /** Coloca armas, faca, granada e C4/kit nos slots fixos da hotbar (o resto, como munição, fica onde está). */
    fun normalize(p: ServerPlayer, shop: List<ShopEntry>) {
        val inv = p.inventory
        for (i in 0 until 36) {
            val stack = inv.getItem(i)
            val target = slotOf(stack, shop) ?: continue
            if (target == i) continue
            if (slotOf(inv.getItem(target), shop) == target) continue
            val other = inv.getItem(target)
            inv.setItem(target, stack)
            inv.setItem(i, other)
        }
    }
}

package com.shifustrike.pointblank

import com.shifustrike.registry.ModItems
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import java.util.UUID

/** Marca de "item do evento" em `minecraft:custom_data` → `shifustrike`. Itens marcados nunca saem da partida. */
object EventItems {
    private const val KEY = "shifustrike"

    fun mark(stack: ItemStack, purchaseId: UUID? = null): ItemStack {
        CustomData.update(DataComponents.CUSTOM_DATA, stack) { root ->
            val tag = CompoundTag()
            tag.putBoolean("evento", true)
            if (purchaseId != null) tag.putUUID("compra", purchaseId)
            root.put(KEY, tag)
        }
        return stack
    }

    private fun tag(stack: ItemStack): CompoundTag? {
        val data = stack.get(DataComponents.CUSTOM_DATA) ?: return null
        val root = data.copyTag()
        return if (root.contains(KEY)) root.getCompound(KEY) else null
    }

    fun isEvent(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (stack.`is`(ModItems.FACA) || stack.`is`(ModItems.C4) || stack.`is`(ModItems.KIT)) return true
        return tag(stack)?.getBoolean("evento") == true
    }

    fun purchaseId(stack: ItemStack): UUID? = tag(stack)?.takeIf { it.hasUUID("compra") }?.getUUID("compra")

    /** Tira do inventário todos os itens do evento. Devolve quantos stacks saíram. */
    fun purge(player: ServerPlayer): Int {
        val inv = player.inventory
        var n = 0
        for (i in 0 until inv.containerSize) {
            if (isEvent(inv.getItem(i))) {
                inv.setItem(i, ItemStack.EMPTY)
                n++
            }
        }
        if (isEvent(player.containerMenu.carried)) player.containerMenu.carried = ItemStack.EMPTY
        return n
    }
}

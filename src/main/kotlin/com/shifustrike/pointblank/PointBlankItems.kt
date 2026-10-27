package com.shifustrike.pointblank

import com.shifustrike.ShifuStrike
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Armas do PointBlank só pelo id do item e pelos componentes de dados — nunca pelas classes dele.
 * A munição carregada fica em `pointblank:custom_tag` → `ammo`; o PointBlank exige `pointblank:ts` junto.
 */
object PointBlankItems {
    private val warned = mutableSetOf<String>()

    private fun item(id: String): Item? {
        val rl = ResourceLocation.tryParse(id)
        val item = rl?.let { BuiltInRegistries.ITEM.getOptional(it).orElse(null) }
        if (item == null && warned.add(id)) ShifuStrike.LOGGER.warn("Item {} não existe (PointBlank carregado?)", id)
        return item
    }

    fun exists(id: String): Boolean = item(id) != null

    @Suppress("UNCHECKED_CAST")
    private fun <T> component(path: String): DataComponentType<T>? =
        BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("pointblank", path)) as DataComponentType<T>?

    /** A arma com o pente cheio. O próprio PointBlank monta a arma (ids, acessórios padrão) no inventoryTick. */
    fun loadedGun(player: ServerPlayer, itemId: String, rounds: Int): ItemStack? {
        val item = item(itemId) ?: return null
        val stack = ItemStack(item)
        item.inventoryTick(stack, player.serverLevel(), player, 0, false)
        component<CompoundTag>("custom_tag")?.let { type ->
            val tag = stack.get(type)?.copy() ?: CompoundTag()
            tag.putInt("ammo", rounds)
            stack.set(type, tag)
        }
        component<Long>("ts")?.let { stack.set(it, System.currentTimeMillis()) }
        return stack
    }

    /** `rounds` balas em itens de munição, divididas pelo tamanho máximo do stack. */
    fun ammo(itemId: String, rounds: Int): List<ItemStack> {
        val item = item(itemId) ?: return emptyList()
        val max = item.defaultMaxStackSize.coerceAtLeast(1)
        val out = mutableListOf<ItemStack>()
        var left = rounds
        while (left > 0) {
            val n = minOf(left, max)
            out += ItemStack(item, n)
            left -= n
        }
        return out
    }

    fun grenade(): ItemStack? = item("pointblank:grenade")?.let { ItemStack(it) }
}

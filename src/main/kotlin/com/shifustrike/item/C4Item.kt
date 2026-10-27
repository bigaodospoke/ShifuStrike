package com.shifustrike.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim

/** C4: segura o clique direito dentro de um bombsite para plantar. */
class C4Item(props: Properties) : Item(props) {
    override fun getUseAnimation(stack: ItemStack): UseAnim = UseAnim.BOW
    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int = 60
}

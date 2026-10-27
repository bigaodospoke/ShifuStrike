package com.shifustrike.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim

/** Kit de desarme: todo CT tem um; segura o clique direito perto da bomba plantada. */
class DefuseKitItem(props: Properties) : Item(props) {
    override fun getUseAnimation(stack: ItemStack): UseAnim = UseAnim.BOW
    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int = 100
}

package com.shifustrike.registry

import com.shifustrike.ShifuStrike
import com.shifustrike.item.C4Item
import com.shifustrike.item.DefuseKitItem
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tiers

object ModItems {
    /** Faca: 8 de dano no total (40 do CS), velocidade de espada. */
    val FACA: Item = SwordItem(Tiers.IRON, Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 5, -2.4f)))
    val C4: Item = C4Item(Item.Properties().stacksTo(1))
    val KIT: Item = DefuseKitItem(Item.Properties().stacksTo(1))

    fun register() {
        Registry.register(BuiltInRegistries.ITEM, ShifuStrike.id("faca"), FACA)
        Registry.register(BuiltInRegistries.ITEM, ShifuStrike.id("c4"), C4)
        Registry.register(BuiltInRegistries.ITEM, ShifuStrike.id("kit_desarme"), KIT)
    }
}

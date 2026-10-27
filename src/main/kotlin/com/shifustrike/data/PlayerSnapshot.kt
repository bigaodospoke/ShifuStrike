package com.shifustrike.data

import com.shifustrike.ShifuStrike
import com.shifustrike.arena.Ponto
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.level.GameType

/** Modificador de atributo que deixa o jogador parado no congelamento do início da rodada. */
val FREEZE_MODIFIER_ID: ResourceLocation = ShifuStrike.id("congelado")

fun applyFreeze(p: ServerPlayer) {
    for (attr in listOf(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH)) {
        val inst = p.getAttribute(attr) ?: continue
        if (!inst.hasModifier(FREEZE_MODIFIER_ID)) {
            inst.addTransientModifier(AttributeModifier(FREEZE_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
        }
    }
}

fun removeFreeze(p: ServerPlayer) {
    p.getAttribute(Attributes.MOVEMENT_SPEED)?.removeModifier(FREEZE_MODIFIER_ID)
    p.getAttribute(Attributes.JUMP_STRENGTH)?.removeModifier(FREEZE_MODIFIER_ID)
}

/** Tudo do jogador que a partida mexe: inventário inteiro, modo de jogo, vida, fome, efeitos e onde ele estava. */
class PlayerSnapshot(val tag: CompoundTag) {
    companion object {
        fun capture(p: ServerPlayer): PlayerSnapshot {
            val t = CompoundTag()
            t.put("inv", p.inventory.save(ListTag()))
            t.putInt("sel", p.inventory.selected)
            t.putInt("modo", p.gameMode.gameModeForPlayer.id)
            t.putFloat("vida", p.health)
            t.putInt("fome", p.foodData.foodLevel)
            t.putFloat("sat", p.foodData.saturationLevel)
            val efeitos = ListTag()
            p.activeEffects.forEach { efeitos.add(it.save()) }
            t.put("efeitos", efeitos)
            t.putString("dim", p.level().dimension().location().toString())
            t.putDouble("x", p.x)
            t.putDouble("y", p.y)
            t.putDouble("z", p.z)
            t.putFloat("yaw", p.yRot)
            t.putFloat("pitch", p.xRot)
            return PlayerSnapshot(t)
        }
    }

    /** Devolve tudo. Vai para `exit` (no `exitLevel`) se for dado; senão, para onde o jogador estava. */
    fun restore(p: ServerPlayer, exit: Ponto?, exitLevel: ServerLevel?) {
        p.stopUsingItem()
        // fecha qualquer tela: a grade de criação e o cursor voltam para o inventário, que é limpo em seguida
        p.closeContainer()
        p.inventory.clearContent()
        p.inventoryMenu.craftSlots.clearContent()
        p.containerMenu.carried = net.minecraft.world.item.ItemStack.EMPTY
        p.inventory.load(tag.getList("inv", Tag.TAG_COMPOUND.toInt()))
        p.inventory.selected = tag.getInt("sel").coerceIn(0, 8)
        removeFreeze(p)
        p.setCamera(p)
        p.setGameMode(GameType.byId(tag.getInt("modo")))
        p.removeAllEffects()
        tag.getList("efeitos", Tag.TAG_COMPOUND.toInt()).forEach { e ->
            MobEffectInstance.load(e as CompoundTag)?.let { p.addEffect(it) }
        }
        p.health = tag.getFloat("vida").coerceIn(1f, p.maxHealth)
        p.foodData.foodLevel = tag.getInt("fome")
        p.foodData.setSaturation(tag.getFloat("sat"))
        p.clearFire()
        p.fallDistance = 0f

        if (exit != null) {
            val level = exitLevel ?: p.serverLevel()
            p.teleportTo(level, exit.x, exit.y, exit.z, exit.yaw, exit.pitch)
        } else {
            val key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("dim")))
            val level = p.server.getLevel(key) ?: p.serverLevel()
            p.teleportTo(level, tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"), tag.getFloat("yaw"), tag.getFloat("pitch"))
        }
        p.inventoryMenu.broadcastChanges()
    }
}

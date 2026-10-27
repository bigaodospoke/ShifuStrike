package com.shifustrike.gametest

import com.shifustrike.arena.Ponto
import com.shifustrike.data.PlayerSnapshot
import com.shifustrike.data.SnapshotStorage
import com.shifustrike.pointblank.EventItems
import com.shifustrike.pointblank.PointBlankItems
import com.shifustrike.registry.ModItems
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType

@Suppress("DEPRECATION")
class SnapshotGameTest : FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun guarda_e_devolve_tudo(h: GameTestHelper) {
        val p = h.makeMockServerPlayerInLevel()
        p.inventory.setItem(7, ItemStack(Items.DIAMOND, 3))
        p.setItemSlot(EquipmentSlot.HEAD, ItemStack(Items.IRON_HELMET))
        p.setGameMode(GameType.SURVIVAL)
        p.health = 13f
        p.foodData.foodLevel = 9
        p.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1))

        SnapshotStorage.save(p.uuid, PlayerSnapshot.capture(p))
        h.assertTrue(SnapshotStorage.has(p.uuid), "arquivo não foi gravado")
        p.inventory.clearContent()
        p.setGameMode(GameType.ADVENTURE)
        p.health = 20f
        p.removeAllEffects()

        SnapshotStorage.load(p.uuid)!!.restore(p, null, null)
        SnapshotStorage.delete(p.uuid)

        h.assertTrue(p.inventory.getItem(7).`is`(Items.DIAMOND) && p.inventory.getItem(7).count == 3, "diamantes não voltaram ao slot 7")
        h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).`is`(Items.IRON_HELMET), "capacete não voltou")
        h.assertTrue(p.gameMode.gameModeForPlayer == GameType.SURVIVAL, "modo=${p.gameMode.gameModeForPlayer}")
        h.assertTrue(p.health == 13f, "vida=${p.health}")
        h.assertTrue(p.foodData.foodLevel == 9, "fome=${p.foodData.foodLevel}")
        h.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SPEED), "efeito não voltou")
        h.assertTrue(!SnapshotStorage.has(p.uuid), "arquivo não foi apagado")
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun restaura_depois_de_reinicio(h: GameTestHelper) {
        val p = h.makeMockServerPlayerInLevel()
        p.inventory.setItem(3, ItemStack(Items.EMERALD, 5))
        p.setGameMode(GameType.SURVIVAL)
        SnapshotStorage.save(p.uuid, PlayerSnapshot.capture(p))

        p.inventory.clearContent()
        p.setGameMode(GameType.ADVENTURE)
        p.inventory.add(ItemStack(ModItems.FACA))
        p.inventory.add(EventItems.mark(PointBlankItems.loadedGun(p, "pointblank:ak47", 30)!!))

        // "reinício": a memória some, só o disco fica
        SnapshotStorage.init(h.level.server)
        val saida = h.absoluteVec(net.minecraft.world.phys.Vec3(4.5, 2.0, 4.5))
        SnapshotStorage.restoreOnJoin(p, Ponto(saida.x, saida.y, saida.z, 0f, 0f), h.level)

        h.assertTrue(p.inventory.getItem(3).`is`(Items.EMERALD) && p.inventory.getItem(3).count == 5, "inventário original não voltou")
        h.assertTrue(p.inventory.countItem(ModItems.FACA) == 0, "faca ficou")
        h.assertTrue(p.gameMode.gameModeForPlayer == GameType.SURVIVAL, "modo=${p.gameMode.gameModeForPlayer}")
        h.assertTrue(p.position().distanceTo(saida) < 0.01, "não foi para a saída: ${p.position()}")
        h.assertTrue(!SnapshotStorage.has(p.uuid), "arquivo ficou")
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun entrar_sem_salvo_tira_itens_do_evento(h: GameTestHelper) {
        val p = h.makeMockServerPlayerInLevel()
        p.inventory.add(ItemStack(Items.APPLE))
        p.inventory.add(ItemStack(ModItems.C4))
        SnapshotStorage.restoreOnJoin(p, null, null)
        h.assertTrue(p.inventory.countItem(ModItems.C4) == 0, "C4 ficou com quem não está na partida")
        h.assertTrue(p.inventory.countItem(Items.APPLE) == 1, "maçã sumiu")
        h.succeed()
    }
}

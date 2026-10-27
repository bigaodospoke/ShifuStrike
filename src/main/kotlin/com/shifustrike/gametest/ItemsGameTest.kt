package com.shifustrike.gametest

import com.shifustrike.pointblank.EventItems
import com.shifustrike.pointblank.PointBlankItems
import com.shifustrike.registry.ModItems
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.UUID

class ItemsGameTest : FabricGameTest {
    @Suppress("UNCHECKED_CAST")
    private fun customTag(stack: ItemStack): CompoundTag? {
        val type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("pointblank:custom_tag")) as DataComponentType<CompoundTag>?
        return type?.let { stack.get(it) }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun ak47_vem_carregada(h: GameTestHelper) {
        val p = h.makeMockServerPlayerInLevel()
        val ak = PointBlankItems.loadedGun(p, "pointblank:ak47", 30)
        h.assertTrue(ak != null, "ak47 nula")
        val tag = customTag(ak!!)
        h.assertTrue(tag != null, "sem pointblank:custom_tag")
        h.assertTrue(tag!!.getInt("ammo") == 30, "ammo=${tag.getInt("ammo")}")
        h.assertTrue(tag.getLong("mid") != 0L, "mid zerado: PointBlank não inicializou a arma")
        val ts = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("pointblank:ts"))
        h.assertTrue(ts != null && ak.has(ts), "sem pointblank:ts")
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun arma_inexistente_volta_nula(h: GameTestHelper) {
        val p = h.makeMockServerPlayerInLevel()
        h.assertTrue(PointBlankItems.loadedGun(p, "pointblank:nao_existe", 30) == null, "deveria ser nula")
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun municao_divide_em_stacks(h: GameTestHelper) {
        val stacks = PointBlankItems.ammo("pointblank:ammo762", 90)
        h.assertTrue(stacks.sumOf { it.count } == 90, "soma=${stacks.sumOf { it.count }}")
        h.assertTrue(stacks.all { it.count <= it.maxStackSize }, "stack acima do máximo")
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun marca_de_evento(h: GameTestHelper) {
        val id = UUID.randomUUID()
        val marcado = EventItems.mark(ItemStack(Items.STONE), id)
        h.assertTrue(EventItems.isEvent(marcado), "pedra marcada deveria ser do evento")
        h.assertTrue(!EventItems.isEvent(ItemStack(Items.STONE)), "pedra comum não é do evento")
        h.assertTrue(EventItems.purchaseId(marcado) == id, "uuid da compra não voltou")
        h.assertTrue(EventItems.isEvent(ItemStack(ModItems.FACA)), "faca é sempre do evento")
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun purge_tira_so_itens_do_evento(h: GameTestHelper) {
        val p = h.makeMockServerPlayerInLevel()
        p.inventory.add(ItemStack(Items.STONE))
        p.inventory.add(ItemStack(ModItems.FACA))
        p.inventory.add(EventItems.mark(PointBlankItems.loadedGun(p, "pointblank:ak47", 30)!!))
        val removidos = EventItems.purge(p)
        h.assertTrue(removidos == 2, "removidos=$removidos")
        h.assertTrue(p.inventory.countItem(Items.STONE) == 1, "a pedra sumiu")
        h.assertTrue((0 until p.inventory.containerSize).count { !p.inventory.getItem(it).isEmpty } == 1, "sobrou item do evento")
        h.succeed()
    }
}

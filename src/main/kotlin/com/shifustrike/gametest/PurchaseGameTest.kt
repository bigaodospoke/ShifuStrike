package com.shifustrike.gametest

import com.shifustrike.core.BuyCheck
import com.shifustrike.core.BuyFail
import com.shifustrike.match.Loadouts
import com.shifustrike.match.Purchases
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.Vec3

class PurchaseGameTest : FabricGameTest {
    private fun item(id: String) = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))

    @Suppress("UNCHECKED_CAST")
    private fun ammoIn(stack: net.minecraft.world.item.ItemStack): Int {
        val t = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("pointblank:custom_tag")) as DataComponentType<CompoundTag>
        return stack.get(t)?.getInt("ammo") ?: -1
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "compra_ak")
    fun compra_ak_desconta_e_entrega(h: GameTestHelper) {
        val (m, _, tr) = Fixtures.started(h)
        m.players.getValue(tr.uuid).money = 3000
        val r = Purchases.buy(m, tr, "ak47")
        h.assertTrue(r is BuyCheck.Ok, "compra=$r")
        val ak = tr.inventory.getItem(Loadouts.SLOT_PRIMARIA)
        h.assertTrue(ak.`is`(item("pointblank:ak47")), "slot 0=$ak")
        h.assertTrue(ammoIn(ak) == 30, "pente=${ammoIn(ak)}")
        h.assertTrue(tr.inventory.countItem(item("pointblank:ammo762")) == 90, "reserva=${tr.inventory.countItem(item("pointblank:ammo762"))}")
        h.assertTrue(m.players.getValue(tr.uuid).money == 300, "dinheiro=${m.players.getValue(tr.uuid).money}")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "compra_dupla")
    fun compra_dupla_no_mesmo_tick(h: GameTestHelper) {
        val (m, _, tr) = Fixtures.started(h)
        m.players.getValue(tr.uuid).money = 3000
        Purchases.buy(m, tr, "ak47")
        val segunda = Purchases.buy(m, tr, "ak47")
        h.assertTrue(segunda == BuyCheck.Fail(BuyFail.JA_TEM), "segunda=$segunda")
        h.assertTrue(m.players.getValue(tr.uuid).money == 300, "dinheiro=${m.players.getValue(tr.uuid).money}")
        h.assertTrue(tr.inventory.countItem(item("pointblank:ak47")) == 1, "ak47=${tr.inventory.countItem(item("pointblank:ak47"))}")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "compra_troca")
    fun trocar_primaria_larga_a_antiga(h: GameTestHelper) {
        val (m, _, tr) = Fixtures.started(h)
        m.players.getValue(tr.uuid).money = 5000
        Purchases.buy(m, tr, "galil")
        Purchases.buy(m, tr, "ak47")
        val chao = h.level.getEntitiesOfClass(ItemEntity::class.java, tr.boundingBox.inflate(3.0)) { it.item.`is`(item("pointblank:ak74")) }
        h.assertTrue(chao.size == 1, "galil no chão=${chao.size}")
        h.assertTrue(tr.inventory.countItem(item("pointblank:ak74")) == 0, "galil ficou no inventário")
        h.assertTrue(tr.inventory.getItem(Loadouts.SLOT_PRIMARIA).`is`(item("pointblank:ak47")), "sem ak47")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "compra_colete")
    fun colete_vira_estado(h: GameTestHelper) {
        val (m, ct, _) = Fixtures.started(h)
        val pt = m.players.getValue(ct.uuid)
        pt.money = 2000
        Purchases.buy(m, ct, "colete")
        h.assertTrue(pt.armor == 100 && !pt.helmet, "armor=${pt.armor} helmet=${pt.helmet}")
        val r = Purchases.buy(m, ct, "colete_capacete")
        h.assertTrue(r == BuyCheck.Ok(350, null), "capacete=$r")
        h.assertTrue(pt.helmet, "sem capacete")
        h.assertTrue(pt.money == 2000 - 650 - 350, "dinheiro=${pt.money}")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "compra_tempo_zona", timeoutTicks = 200)
    fun fora_do_tempo_e_da_zona(h: GameTestHelper) {
        val (m, _, tr) = Fixtures.started(h, Fixtures.config { segundosCongelado = 1; segundosCompra = 2; raioZonaCompra = 3.0 })
        m.players.getValue(tr.uuid).money = 16000
        val s = m.arena.spawnsTr[0]
        tr.teleportTo(h.level, s.x, s.y, s.z + 5.0, 0f, 0f)
        h.assertTrue(Purchases.buy(m, tr, "deagle") == BuyCheck.Fail(BuyFail.FORA_DA_ZONA), "longe do spawn comprou")
        tr.teleportTo(h.level, s.x, s.y, s.z, 0f, 0f)
        h.runAfterDelay(45) {
            h.assertTrue(Purchases.buy(m, tr, "deagle") == BuyCheck.Fail(BuyFail.FORA_DO_TEMPO), "comprou depois do tempo")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "compra_reembolso", timeoutTicks = 200)
    fun reembolso_devolve(h: GameTestHelper) {
        val (m, _, tr) = Fixtures.started(h, Fixtures.config { segundosCongelado = 1; segundosCompra = 2 })
        val pt = m.players.getValue(tr.uuid)
        pt.money = 3000
        Purchases.buy(m, tr, "ak47")
        h.assertTrue(Purchases.refund(m, tr, "ak47"), "reembolso recusado")
        h.assertTrue(pt.money == 3000, "dinheiro=${pt.money}")
        h.assertTrue(tr.inventory.countItem(item("pointblank:ak47")) == 0, "ak47 ficou")
        h.assertTrue(tr.inventory.countItem(item("pointblank:ammo762")) == 0, "munição da ak47 ficou")
        h.assertTrue(pt.purchases.isEmpty(), "compra continua registrada")
        Purchases.buy(m, tr, "ak47")
        h.runAfterDelay(45) {
            h.assertTrue(!Purchases.refund(m, tr, "ak47"), "reembolsou depois do tempo de compra")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "compra_zona_vec")
    fun zona_de_compra_e_do_proprio_time(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, Fixtures.config { raioZonaCompra = 3.0 })
        val s = m.arena.spawnsCt[0]
        tr.teleportTo(h.level, s.x, s.y, s.z, 0f, 0f)
        h.assertTrue(!Purchases.inBuyZone(m, tr), "TR comprando no spawn do CT")
        h.assertTrue(Purchases.inBuyZone(m, ct), "CT fora da própria zona")
        h.assertTrue(ct.position().distanceTo(Vec3(s.x, s.y, s.z)) < 1.0, "CT longe do spawn")
        Fixtures.reset(h)
        h.succeed()
    }
}

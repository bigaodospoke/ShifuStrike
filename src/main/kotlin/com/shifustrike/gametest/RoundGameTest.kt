package com.shifustrike.gametest

import com.shifustrike.core.Team
import com.shifustrike.gametest.Fixtures.modo
import com.shifustrike.match.Loadouts
import com.shifustrike.match.Match
import com.shifustrike.registry.ModItems
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType

class RoundGameTest : FabricGameTest {
    private fun rapido() = Fixtures.config {
        segundosCongelado = 1
        segundosCompra = 2
        segundosRodada = 3
        segundosFimRodada = 1
    }

    private fun item(id: String) = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rodada_loadout")
    fun comeca_com_faca_e_pistola(h: GameTestHelper) {
        val (_, ct, tr) = Fixtures.started(h, rapido())
        h.assertTrue(tr.inventory.getItem(Loadouts.SLOT_PISTOLA).`is`(item("pointblank:glock18")), "TR sem glock no slot 1: ${tr.inventory.getItem(1)}")
        h.assertTrue(tr.inventory.getItem(Loadouts.SLOT_FACA).`is`(ModItems.FACA), "TR sem faca no slot 2")
        h.assertTrue(tr.inventory.countItem(item("pointblank:ammo9mm")) == 120, "reserva TR=${tr.inventory.countItem(item("pointblank:ammo9mm"))}")
        h.assertTrue(ct.inventory.getItem(Loadouts.SLOT_PISTOLA).`is`(item("pointblank:usp45")), "CT sem usp no slot 1")
        h.assertTrue(ct.inventory.getItem(Loadouts.SLOT_FACA).`is`(ModItems.FACA), "CT sem faca")
        h.assertTrue(ct.inventory.getItem(Loadouts.SLOT_EXTRA).`is`(ModItems.KIT), "CT sem kit no slot 4")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rodada_congela")
    fun congela_no_inicio(h: GameTestHelper) {
        val (_, ct, _) = Fixtures.started(h, rapido())
        h.assertTrue(ct.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.0, "velocidade no congelamento=${ct.getAttributeValue(Attributes.MOVEMENT_SPEED)}")
        h.runAfterDelay(30) {
            h.assertTrue(ct.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.05, "continua congelado")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rodada_tempo", timeoutTicks = 200)
    fun tempo_acaba_ct_ganha(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, rapido())
        h.runAfterDelay(90) {
            h.assertTrue(m.score[Team.CT] == 1, "placar CT=${m.score[Team.CT]}")
            h.assertTrue(m.players.getValue(ct.uuid).money == 500 + 3250, "CT=${m.players.getValue(ct.uuid).money}")
            h.assertTrue(m.players.getValue(tr.uuid).money == 500 + 1400, "TR=${m.players.getValue(tr.uuid).money}")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rodada_sobrevivente", timeoutTicks = 200)
    fun sobrevivente_mantem_arma(h: GameTestHelper) {
        val (m, _, tr) = Fixtures.started(h, rapido())
        Loadouts.giveWeapon(tr, m.shop.first { it.chave == "ak47" }, null)
        h.runAfterDelay(115) {
            h.assertTrue(m.round == 2, "rodada=${m.round}")
            h.assertTrue(tr.inventory.getItem(Loadouts.SLOT_PRIMARIA).`is`(item("pointblank:ak47")), "ak47 sumiu: ${tr.inventory.getItem(0)}")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rodada_fim_partida", timeoutTicks = 300)
    fun cinco_vitorias_acaba(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, rapido().apply { vitoriasParaVencer = 2 })
        h.runAfterDelay(220) {
            h.assertTrue(m.state == Match.State.ENDED, "estado=${m.state} placar=${m.score}")
            for (p in listOf(ct, tr)) {
                h.assertTrue(modo(p) == GameType.SURVIVAL, "modo=${modo(p)}")
                h.assertTrue(p.inventory.countItem(Items.DIAMOND) == 1, "inventário não voltou")
                h.assertTrue(p.inventory.countItem(ModItems.FACA) == 0, "faca ficou")
            }
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rodada_regen", timeoutTicks = 300)
    fun sem_regeneracao(h: GameTestHelper) {
        val (_, ct, _) = Fixtures.started(h, Fixtures.config())
        ct.health = 10f
        // jogador falso não roda o tick do jogador sozinho: simula a fome/regeneração do Minecraft a cada tick
        h.onEachTick { ct.foodData.tick(ct) }
        h.runAfterDelay(200) {
            h.assertTrue(ct.health == 10f, "vida regenerou: ${ct.health}")
            Fixtures.reset(h)
            h.succeed()
        }
    }
}

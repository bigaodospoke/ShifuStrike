package com.shifustrike.gametest

import com.shifustrike.core.BombState
import com.shifustrike.core.Phase
import com.shifustrike.core.Team
import com.shifustrike.core.WinReason
import com.shifustrike.match.Loadouts
import com.shifustrike.match.MatchManager
import com.shifustrike.registry.ModItems
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.AABB

/** Dano e plantio esperam 65 ticks (congelamento de 1 s + invulnerabilidade de quem acabou de entrar). */
class BombGameTest : FabricGameTest {
    private fun cfg() = Fixtures.config { segundosCongelado = 1; segundosCompra = 2; segundosBomba = 2 }

    private fun c4NoChao(h: GameTestHelper): List<ItemEntity> {
        val box = MatchManager.match!!.arena.area!!
        val aabb = AABB(box.minX.toDouble(), box.minY.toDouble(), box.minZ.toDouble(), box.maxX + 1.0, box.maxY + 1.0, box.maxZ + 1.0)
        return h.level.getEntitiesOfClass(ItemEntity::class.java, aabb) { it.item.`is`(ModItems.C4) }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "bomba_sorteio")
    fun c4_vai_para_um_tr(h: GameTestHelper) {
        val (m, ps) = Fixtures.startedN(h, 4, cfg())
        val comC4 = ps.filter { it.inventory.countItem(ModItems.C4) > 0 }
        h.assertTrue(comC4.size == 1, "portadores=${comC4.size}")
        h.assertTrue(m.players.getValue(comC4[0].uuid).team == Team.TR, "C4 com CT")
        h.assertTrue(comC4[0].inventory.getItem(Loadouts.SLOT_EXTRA).`is`(ModItems.C4), "C4 fora do slot 4")
        Fixtures.reset(h)
        h.succeed()
    }

    /** TR que sobrevive com a C4 não pode gerar uma segunda bomba na rodada seguinte. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "bomba_unica", timeoutTicks = 300)
    fun c4_unica_depois_da_rodada(h: GameTestHelper) {
        val (m, ps) = Fixtures.startedN(h, 4, Fixtures.config { segundosCongelado = 1; segundosCompra = 2; segundosRodada = 3; segundosFimRodada = 1 })
        // os dois TR sobrevivem com uma C4 cada (pior caso de sobra)
        m.online().filter { it.first.team == Team.TR }.forEach { it.second.inventory.setItem(Loadouts.SLOT_EXTRA, net.minecraft.world.item.ItemStack(ModItems.C4)) }
        h.runAfterDelay(115) {
            h.assertTrue(m.round == 2, "rodada=${m.round}")
            val total = ps.sumOf { it.inventory.countItem(ModItems.C4) }
            h.assertTrue(total == 1, "C4 na rodada 2: $total")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "bomba_onde", timeoutTicks = 200)
    fun plantar_fora_do_site_nao_deixa(h: GameTestHelper) {
        val (m, _, tr) = Fixtures.started(h, cfg())
        h.runAfterDelay(25) {
            tr.setOnGround(true)
            h.assertTrue(!m.bomb.canPlant(tr), "deixou plantar no spawn")
            Fixtures.noSiteA(h, tr)
            h.assertTrue(m.bomb.canPlant(tr), "não deixou plantar no site")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "bomba_explode", timeoutTicks = 300)
    fun plantada_explode_e_tr_ganha(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, cfg())
        h.runAfterDelay(65) {
            Fixtures.noSiteA(h, tr)
            m.bomb.plant(tr)
            h.assertTrue(m.bomb.state == BombState.PLANTED, "estado=${m.bomb.state}")
            h.assertTrue(tr.inventory.countItem(ModItems.C4) == 0, "C4 continuou no inventário")
            Fixtures.noSiteA(h, ct)
            h.runAfterDelay(50) {
                h.assertTrue(m.score[Team.TR] == 1, "placar=${m.score}")
                h.assertTrue(m.lastOutcome?.reason == WinReason.BOMB_EXPLODED, "razão=${m.lastOutcome}")
                h.assertTrue(!m.players.getValue(ct.uuid).alive, "CT ao lado da bomba sobreviveu")
                Fixtures.reset(h)
                h.succeed()
            }
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "bomba_desarme", timeoutTicks = 300)
    fun desarmada_ct_ganha(h: GameTestHelper) {
        val (m, ct, _) = Fixtures.started(h, cfg().apply { segundosBomba = 20 })
        val tr = m.online().first { it.first.team == Team.TR }.second
        h.runAfterDelay(65) {
            Fixtures.noSiteA(h, tr)
            m.bomb.plant(tr)
            Fixtures.noSiteA(h, ct)
            h.assertTrue(m.bomb.canDefuse(ct), "não deixou desarmar ao lado da bomba")
            m.bomb.defuse(ct)
            h.runAfterDelay(2) {
                h.assertTrue(m.score[Team.CT] == 1 && m.lastOutcome?.reason == WinReason.BOMB_DEFUSED, "placar=${m.score} razão=${m.lastOutcome}")
                h.assertTrue(m.players.getValue(ct.uuid).money == 500 + 3500, "CT=${m.players.getValue(ct.uuid).money}")
                Fixtures.reset(h)
                h.succeed()
            }
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "bomba_bonus", timeoutTicks = 300)
    fun tr_perde_depois_de_plantar_ganha_bonus(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, cfg().apply { segundosBomba = 20 })
        h.runAfterDelay(65) {
            Fixtures.noSiteA(h, tr)
            m.bomb.plant(tr)
            Fixtures.noSiteA(h, ct)
            m.bomb.defuse(ct)
            h.runAfterDelay(2) {
                h.assertTrue(m.players.getValue(tr.uuid).money == 500 + 1400 + 800, "TR=${m.players.getValue(tr.uuid).money}")
                Fixtures.reset(h)
                h.succeed()
            }
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "bomba_continua", timeoutTicks = 300)
    fun tr_mortos_com_bomba_continua(h: GameTestHelper) {
        val (m, _, tr) = Fixtures.started(h, cfg().apply { segundosBomba = 20 })
        h.runAfterDelay(65) {
            Fixtures.noSiteA(h, tr)
            m.bomb.plant(tr)
            tr.hurt(tr.damageSources().fall(), 1000f)
            h.assertTrue(!m.players.getValue(tr.uuid).alive, "TR não morreu")
            h.runAfterDelay(10) {
                h.assertTrue(m.clock.phase == Phase.PLANTED, "rodada acabou com a bomba plantada: ${m.clock.phase}")
                Fixtures.reset(h)
                h.succeed()
            }
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "bomba_ct_nao_pega", timeoutTicks = 200)
    fun ct_nao_pega_c4(h: GameTestHelper) {
        val (_, ct, tr) = Fixtures.started(h, cfg())
        tr.inventory.clearContent()
        val e = ItemEntity(h.level, ct.x, ct.y, ct.z, net.minecraft.world.item.ItemStack(ModItems.C4))
        e.setNoPickUpDelay()
        h.level.addFreshEntity(e)
        e.playerTouch(ct)
        h.assertTrue(ct.inventory.countItem(ModItems.C4) == 0, "CT pegou a C4")
        h.assertTrue(!e.isRemoved, "C4 sumiu do chão")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "bomba_desconecta", timeoutTicks = 200)
    fun c4_cai_quando_portador_desconecta(h: GameTestHelper) {
        val (m, ps) = Fixtures.startedN(h, 4, cfg())
        val portador = ps.first { it.inventory.countItem(ModItems.C4) > 0 }
        MatchManager.onDisconnect(portador)
        h.assertTrue(c4NoChao(h).size == 1, "C4 no chão=${c4NoChao(h).size}")
        h.assertTrue(m.clock.phase != Phase.ROUND_END, "a rodada acabou")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "bomba_perdida", timeoutTicks = 200)
    fun c4_perdida_volta_para_um_tr(h: GameTestHelper) {
        val (m, ps) = Fixtures.startedN(h, 4, cfg())
        val portador = ps.first { it.inventory.countItem(ModItems.C4) > 0 }
        portador.inventory.removeItemNoUpdate(Loadouts.SLOT_EXTRA)
        h.runAfterDelay(25) {
            val agora = ps.filter { it.inventory.countItem(ModItems.C4) > 0 }
            h.assertTrue(agora.size == 1 && m.players.getValue(agora[0].uuid).team == Team.TR, "C4 não voltou para um TR")
            Fixtures.reset(h)
            h.succeed()
        }
    }
}

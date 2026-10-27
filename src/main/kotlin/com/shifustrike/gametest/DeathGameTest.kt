package com.shifustrike.gametest

import com.shifustrike.core.Team
import com.shifustrike.gametest.Fixtures.modo
import com.shifustrike.match.Loadouts
import com.shifustrike.match.Match
import com.shifustrike.match.MatchManager
import com.shifustrike.pointblank.EventItems
import com.shifustrike.registry.ModItems
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.GameType

/** Testes que causam dano esperam 65 ticks: fim do congelamento (1 s) e dos 3 s de invulnerabilidade de quem acabou de entrar no servidor. */
class DeathGameTest : FabricGameTest {
    private fun cfg() = Fixtures.config { segundosCongelado = 1; segundosCompra = 2 }
    private fun item(id: String) = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))

    private fun arma(m: Match, p: ServerPlayer, chave: String) {
        Loadouts.giveWeapon(p, m.shop.first { it.chave == chave }, null)
        p.inventory.selected = Loadouts.SLOT_PRIMARIA
    }

    private fun matar(vitima: ServerPlayer, atacante: ServerPlayer) = vitima.hurt(atacante.damageSources().playerAttack(atacante), 1000f)

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "morte_rifle", timeoutTicks = 200)
    fun kill_de_rifle_paga_300(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, cfg())
        arma(m, ct, "m4a4")
        h.runAfterDelay(65) {
            matar(tr, ct)
            val vt = m.players.getValue(tr.uuid)
            val vc = m.players.getValue(ct.uuid)
            h.assertTrue(!vt.alive, "TR continua vivo")
            h.assertTrue(modo(tr) == GameType.SPECTATOR, "modo=${modo(tr)}")
            h.assertTrue(vt.deaths == 1, "mortes=${vt.deaths}")
            h.assertTrue(tr.camera === ct, "câmera do morto não está no vivo")
            h.assertTrue(vc.kills == 1, "kills=${vc.kills}")
            h.assertTrue(vc.money == 800, "dinheiro CT=${vc.money}")
            h.runAfterDelay(3) {
                h.assertTrue(m.score[Team.CT] == 1, "placar=${m.score}")
                Fixtures.reset(h)
                h.succeed()
            }
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "morte_faca", timeoutTicks = 200)
    fun kill_de_faca_paga_1500(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, cfg())
        ct.inventory.selected = Loadouts.SLOT_FACA
        h.runAfterDelay(65) {
            matar(tr, ct)
            h.assertTrue(m.players.getValue(ct.uuid).money == 2000, "dinheiro CT=${m.players.getValue(ct.uuid).money}")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "morte_awp", timeoutTicks = 200)
    fun kill_de_awp_paga_100(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, cfg())
        arma(m, ct, "awp")
        h.runAfterDelay(65) {
            matar(tr, ct)
            h.assertTrue(m.players.getValue(ct.uuid).money == 600, "dinheiro CT=${m.players.getValue(ct.uuid).money}")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "morte_larga", timeoutTicks = 200)
    fun morto_larga_a_melhor_arma(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, cfg())
        arma(m, tr, "ak47")
        h.runAfterDelay(65) {
            val pos = tr.position()
            matar(tr, ct)
            val chao = h.level.getEntitiesOfClass(ItemEntity::class.java, tr.boundingBox.move(pos.subtract(tr.position())).inflate(3.0))
            val ak = chao.filter { it.item.`is`(item("pointblank:ak47")) }
            h.assertTrue(ak.size == 1 && EventItems.isEvent(ak[0].item), "ak47 no chão: ${ak.size}")
            h.assertTrue(chao.none { it.item.`is`(item("pointblank:glock18")) }, "a glock também caiu")
            h.assertTrue(chao.none { it.item.`is`(ModItems.FACA) }, "a faca caiu")
            h.assertTrue(tr.inventory.isEmpty, "o morto ficou com itens")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "morte_queda", timeoutTicks = 200)
    fun queda_nao_paga_ninguem(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, cfg())
        h.runAfterDelay(65) {
            tr.hurt(tr.damageSources().fall(), 1000f)
            h.assertTrue(!m.players.getValue(tr.uuid).alive, "TR não morreu da queda")
            h.assertTrue(m.players.getValue(ct.uuid).money == 500, "CT ganhou dinheiro: ${m.players.getValue(ct.uuid).money}")
            h.assertTrue(m.players.getValue(ct.uuid).kills == 0, "contou kill")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "morte_desconecta", timeoutTicks = 200)
    fun desconectar_conta_como_morte(h: GameTestHelper) {
        val (m, _, tr) = Fixtures.started(h, cfg())
        MatchManager.onDisconnect(tr)
        val pt = m.players.getValue(tr.uuid)
        h.assertTrue(!pt.alive && !pt.connected, "alive=${pt.alive} connected=${pt.connected}")
        MatchManager.onJoin(tr)
        h.assertTrue(pt.connected && !pt.alive, "voltou vivo")
        h.assertTrue(modo(tr) == GameType.SPECTATOR, "modo=${modo(tr)}")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "morte_cancelar", timeoutTicks = 200)
    fun cancelar_com_jogador_morto_restaura(h: GameTestHelper) {
        val (_, ct, tr) = Fixtures.started(h, cfg())
        h.runAfterDelay(65) {
            matar(tr, ct)
            MatchManager.cancel(h.level.server)
            h.assertTrue(modo(tr) == GameType.SURVIVAL, "modo=${modo(tr)}")
            h.assertTrue(tr.camera === tr, "câmera ficou no outro")
            h.assertTrue(tr.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.05, "ficou congelado")
            h.succeed()
        }
    }
}

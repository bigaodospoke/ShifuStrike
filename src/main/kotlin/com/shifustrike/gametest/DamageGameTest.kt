package com.shifustrike.gametest

import com.shifustrike.core.Team
import com.shifustrike.pointblank.HeadshotFlag
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

/** Dano só depois de 65 ticks (invulnerabilidade de quem acabou de entrar no servidor). */
class DamageGameTest : FabricGameTest {
    private fun cfg() = Fixtures.config { segundosCongelado = 1; segundosCompra = 2 }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dano_amigo", timeoutTicks = 200)
    fun sem_fogo_amigo(h: GameTestHelper) {
        val (m, _) = Fixtures.startedN(h, 4, cfg())
        val cts = m.online().filter { it.first.team == Team.CT }.map { it.second }
        h.runAfterDelay(65) {
            cts[1].hurt(cts[0].damageSources().playerAttack(cts[0]), 5f)
            h.assertTrue(cts[1].health == 20f, "fogo amigo: vida=${cts[1].health}")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dano_fora", timeoutTicks = 200)
    fun de_fora_nao_machuca(h: GameTestHelper) {
        val (_, ct, _) = Fixtures.started(h, cfg())
        val fora = Fixtures.player(h)
        h.runAfterDelay(65) {
            ct.hurt(fora.damageSources().playerAttack(fora), 5f)
            h.assertTrue(ct.health == 20f, "quem está fora feriu: vida=${ct.health}")
            fora.hurt(ct.damageSources().playerAttack(ct), 5f)
            h.assertTrue(fora.health == 20f, "participante feriu quem está fora: vida=${fora.health}")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dano_congelado", timeoutTicks = 200)
    fun congelado_nao_toma_dano(h: GameTestHelper) {
        val (_, ct, tr) = Fixtures.started(h, Fixtures.config { segundosCongelado = 5; segundosCompra = 6 })
        h.runAfterDelay(70) {
            tr.hurt(ct.damageSources().playerAttack(ct), 5f)
            h.assertTrue(tr.health == 20f, "dano no congelamento: vida=${tr.health}")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dano_colete", timeoutTicks = 200)
    fun colete_corta_metade(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, cfg())
        val pt = m.players.getValue(tr.uuid)
        pt.armor = 100
        h.runAfterDelay(65) {
            tr.hurt(ct.damageSources().playerAttack(ct), 10f)
            h.assertTrue(tr.health == 15f, "vida=${tr.health}")
            h.assertTrue(pt.armor == 88, "colete=${pt.armor}")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dano_headshot", timeoutTicks = 200)
    fun headshot_sem_capacete_ignora_colete(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h, cfg())
        val pt = m.players.getValue(tr.uuid)
        pt.armor = 100
        h.runAfterDelay(65) {
            HeadshotFlag.set(true)
            tr.hurt(ct.damageSources().playerAttack(ct), 10f)
            h.assertTrue(tr.health == 10f, "vida=${tr.health}")
            h.assertTrue(pt.armor == 100, "colete=${pt.armor}")
            h.assertTrue(pt.lastHitHeadshot, "headshot não registrado")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dano_queda", timeoutTicks = 200)
    fun queda_ignora_colete(h: GameTestHelper) {
        val (m, _, tr) = Fixtures.started(h, cfg())
        val pt = m.players.getValue(tr.uuid)
        pt.armor = 100
        h.runAfterDelay(65) {
            tr.hurt(tr.damageSources().fall(), 10f)
            h.assertTrue(tr.health == 10f, "vida=${tr.health}")
            h.assertTrue(pt.armor == 100, "colete=${pt.armor}")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    /** O mixin do headshot tem require = 0 (não derruba o servidor se o PointBlank mudar); aqui confirmamos que ele aplicou. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun headshot_mixin_aplicado(h: GameTestHelper) {
        val cls = Class.forName("com.vicmatskiv.pointblank.item.HurtingItem")
        h.assertTrue(cls.declaredMethods.any { "guardaHeadshot" in it.name }, "HurtingItemMixin não aplicou no PointBlank")
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dano_granada_propria", timeoutTicks = 200)
    fun granada_propria_machuca(h: GameTestHelper) {
        val (_, _, tr) = Fixtures.started(h, cfg())
        h.runAfterDelay(65) {
            tr.hurt(tr.damageSources().explosion(tr, tr), 5f)
            h.assertTrue(tr.health == 15f, "vida=${tr.health}")
            Fixtures.reset(h)
            h.succeed()
        }
    }
}

package com.shifustrike.gametest

import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/**
 * PvP da partida dentro de um claim de admin do Flan (o servidor usa Flan). Usa a API do Flan por reflexão:
 * ele só está no runtime de dev. Sem Flan carregado os testes passam sem checar nada.
 */
class FlanGameTest : FabricGameTest {
    private val events = runCatching { Class.forName("io.github.flemmli97.flan.event.EntityInteractEvents") }.getOrNull()

    private fun preventDamage(target: Entity, source: DamageSource): Boolean =
        events!!.getMethod("preventDamage", Entity::class.java, DamageSource::class.java).invoke(null, target, source) as Boolean

    private fun attackSimple(p: Player, target: Entity): InteractionResult =
        events!!.getMethod("attackSimple", Player::class.java, Entity::class.java, Boolean::class.javaPrimitiveType).invoke(null, p, target, false) as InteractionResult

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun flan_mixin_aplicado(h: GameTestHelper) {
        if (events != null) {
            h.assertTrue(events.declaredMethods.any { "ataqueDaPartida" in it.name }, "FlanPvpMixin não aplicou (attackSimple)")
            h.assertTrue(events.declaredMethods.any { "danoDaPartida" in it.name }, "FlanPvpMixin não aplicou (preventDamage)")
        }
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "flan_pvp", timeoutTicks = 200)
    fun pvp_da_partida_dentro_de_claim(h: GameTestHelper) {
        if (events == null) {
            h.succeed()
            return
        }
        val (_, ct, tr) = Fixtures.started(h, Fixtures.config { segundosCongelado = 1; segundosCompra = 2 })
        val level = h.level
        val storageClass = Class.forName("io.github.flemmli97.flan.claim.ClaimStorage")
        val st = storageClass.getMethod("get", ServerLevel::class.java).invoke(null, level)
        val claim = storageClass.getMethod("createAdminClaim", BlockPos::class.java, BlockPos::class.java, ServerLevel::class.java, Boolean::class.javaPrimitiveType)
            .invoke(st, h.absolutePos(BlockPos(-4, -2, -4)), h.absolutePos(BlockPos(12, 10, 12)), level, false)
        h.assertTrue(claim != null, "o Flan não criou o claim de teste")
        h.runAfterDelay(65) {
            try {
                h.assertTrue(attackSimple(ct, tr) == InteractionResult.PASS, "Flan barrou o ataque da partida")
                h.assertTrue(!preventDamage(tr, ct.damageSources().playerAttack(ct)), "Flan barrou o tiro da partida")
                h.assertTrue(!preventDamage(tr, level.damageSources().explosion(ct, ct)), "Flan barrou a granada da partida")
                h.assertTrue(AttackEntityCallback.EVENT.invoker().interact(ct, level, InteractionHand.MAIN_HAND, tr, null) != InteractionResult.FAIL, "faca barrada")
                val src = ct.damageSources().playerAttack(ct)
                val ok = tr.hurt(src, 4f)
                h.assertTrue(tr.health < 20f, "o tiro não feriu: vida=${tr.health} hurt=$ok invul=${tr.isInvulnerableTo(src)}")
            } finally {
                val modo = Class.forName("io.github.flemmli97.flan.player.ClaimEditingMode").getField("DEFAULT").get(null)
                storageClass.getMethod("deleteClaim", Class.forName("io.github.flemmli97.flan.claim.Claim"), Boolean::class.javaPrimitiveType, modo.javaClass, ServerLevel::class.java)
                    .invoke(st, claim, true, modo, level)
                Fixtures.reset(h)
            }
            h.succeed()
        }
    }
}

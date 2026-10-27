package com.shifustrike.guard

import com.shifustrike.core.ArmorMath
import com.shifustrike.match.MatchManager
import com.shifustrike.match.Match
import com.shifustrike.pointblank.HeadshotFlag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

/** Quem pode ferir quem durante a partida, e o colete do CS. */
object DamageGuard {
    fun allowDamage(victim: LivingEntity, source: DamageSource): Boolean {
        val m = MatchManager.match?.takeIf { it.state == Match.State.RUNNING } ?: return true
        val attacker = source.entity as? ServerPlayer
        val vp = (victim as? ServerPlayer)?.let { m.players[it.uuid] }
        val ap = attacker?.let { m.players[it.uuid] }
        return when {
            vp == null && ap == null -> true
            vp == null || (attacker != null && ap == null) -> false
            attacker == null -> true
            !vp.alive || !ap!!.alive -> false
            attacker === victim -> true
            ap.team == vp.team -> false
            m.clock.frozen -> false
            else -> true
        }
    }

    /** Chamado pelo mixin depois da armadura do Minecraft: aplica o colete do CS e devolve o dano em vida do Minecraft. */
    fun afterArmor(victim: LivingEntity, source: DamageSource, amount: Float): Float {
        val p = victim as? ServerPlayer ?: return amount
        val pt = MatchManager.participant(p) ?: return amount
        if (source.entity !is Player && !source.`is`(DamageTypeTags.IS_EXPLOSION)) return amount
        val m = MatchManager.match ?: return amount
        val headshot = HeadshotFlag.consume()
        pt.lastHitHeadshot = headshot
        val r = ArmorMath.apply(amount * 5f, pt.armor, pt.helmet, headshot, m.cfg.proporcaoColete, m.cfg.bonusColete)
        pt.armor = r.armorLeft
        return r.healthDamage / 5f
    }
}

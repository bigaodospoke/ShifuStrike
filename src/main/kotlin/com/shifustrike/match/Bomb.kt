package com.shifustrike.match

import com.shifustrike.core.BombState
import com.shifustrike.core.Phase
import com.shifustrike.core.Team
import com.shifustrike.registry.ModItems
import net.minecraft.ChatFormatting
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID

/** A C4 da rodada: quem carrega, onde foi plantada, os bips, a explosão e o desarme. */
class Bomb(private val m: Match) {
    var state = BombState.NONE
        private set
    var plantedPos: Vec3? = null
        private set
    var site: Char? = null
        private set
    private var display: UUID? = null
    private var nextBeep = 0

    private fun areaBox(): AABB? = m.arena.area?.let {
        AABB(it.minX.toDouble(), it.minY.toDouble(), it.minZ.toDouble(), it.maxX + 1.0, it.maxY + 1.0, it.maxZ + 1.0)
    }

    private fun groundC4(): List<ItemEntity> = areaBox()?.let { box ->
        m.level.getEntitiesOfClass(ItemEntity::class.java, box) { it.item.`is`(ModItems.C4) }
    } ?: emptyList()

    fun carrier(): ServerPlayer? = m.online().firstOrNull { (pt, p) -> pt.alive && pt.team == Team.TR && p.inventory.countItem(ModItems.C4) > 0 }?.second

    /** Uma C4 só por rodada: tira de todo mundo (sobrevivente que ficou com ela inclusive) e dá a um TR sorteado. */
    fun giveToRandomTr() {
        for ((_, p) in m.online()) {
            val inv = p.inventory
            for (i in 0 until inv.containerSize) if (inv.getItem(i).`is`(ModItems.C4)) inv.setItem(i, ItemStack.EMPTY)
            if (p.containerMenu.carried.`is`(ModItems.C4)) p.containerMenu.carried = ItemStack.EMPTY
        }
        val trs = m.online().filter { (pt, _) -> pt.alive && pt.team == Team.TR }.map { it.second }
        val p = trs.randomOrNull(m.random) ?: return
        p.inventory.setItem(Loadouts.SLOT_EXTRA, ItemStack(ModItems.C4))
        Msg.actionBar(p, "Você está com a bomba!")
    }

    fun siteAt(pos: Vec3): Char? = when {
        m.arena.bombaA?.contains(pos.x, pos.y, pos.z) == true -> 'A'
        m.arena.bombaB?.contains(pos.x, pos.y, pos.z) == true -> 'B'
        else -> null
    }

    fun canPlant(p: ServerPlayer): Boolean {
        val pt = m.players[p.uuid] ?: return false
        return m.state == Match.State.RUNNING && pt.alive && pt.team == Team.TR && state == BombState.NONE &&
            m.clock.phase == Phase.LIVE && p.onGround() && siteAt(p.position()) != null
    }

    /** Planta a C4 onde o jogador está. Devolve false se não podia. */
    fun plant(p: ServerPlayer): Boolean {
        if (!canPlant(p)) return false
        val inv = p.inventory
        for (i in 0 until inv.containerSize) if (inv.getItem(i).`is`(ModItems.C4)) inv.setItem(i, ItemStack.EMPTY)
        val pos = Vec3(p.x, p.y, p.z)
        val d = Display.ItemDisplay(EntityType.ITEM_DISPLAY, m.level)
        d.getSlot(0).set(ItemStack(ModItems.C4))
        d.moveTo(pos.x, pos.y + 0.15, pos.z, 0f, 0f)
        d.addTag(TAG)
        m.level.addFreshEntity(d)
        display = d.uuid
        plantedPos = pos
        site = siteAt(pos)
        state = BombState.PLANTED
        m.clock.plantBomb()
        m.bombWasPlanted = true
        nextBeep = 0
        val title = Component.literal("Bomba plantada").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
        m.online().forEach { (_, sp) -> Msg.title(sp, title, Component.literal("Bombsite $site"), stay = 40) }
        return true
    }

    fun canDefuse(p: ServerPlayer): Boolean {
        val pt = m.players[p.uuid] ?: return false
        val pos = plantedPos ?: return false
        return m.state == Match.State.RUNNING && pt.alive && pt.team == Team.CT && state == BombState.PLANTED &&
            m.clock.phase == Phase.PLANTED && p.position().distanceTo(pos) <= m.cfg.raioDesarme
    }

    fun defuse(p: ServerPlayer) {
        if (!canDefuse(p)) return
        state = BombState.DEFUSED
        removeDisplay()
        m.broadcast("${p.gameProfile.name} desarmou a bomba!")
    }

    fun tick() {
        if (m.state != Match.State.RUNNING) return
        when {
            state == BombState.PLANTED && m.clock.phase == Phase.PLANTED -> {
                if (m.clock.bombExploded) explode() else beep()
            }
            state == BombState.NONE && (m.clock.phase == Phase.FREEZE || m.clock.phase == Phase.LIVE) && m.clock.ticks % 20 == 0 -> {
                // a C4 sumiu (caiu no vazio, despawn): volta para um TR vivo
                if (carrier() == null && groundC4().isEmpty()) giveToRandomTr()
            }
        }
    }

    /** Bip que acelera: de 1 por segundo até 10 por segundo no fim. */
    private fun beep() {
        val pos = plantedPos ?: return
        if (--nextBeep > 0) return
        val total = m.cfg.timings().bombTicks.toDouble()
        val frac = (m.clock.bombTimeLeft / total).coerceIn(0.0, 1.0)
        nextBeep = (2 + 18 * frac).toInt()
        m.level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 2f, 1.8f)
    }

    private fun explode() {
        val pos = plantedPos ?: return
        state = BombState.EXPLODED
        removeDisplay()
        m.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1, pos.z, 2, 1.0, 1.0, 1.0, 0.0)
        m.level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 6f, 0.8f)
        val r = m.cfg.raioExplosaoC4
        for ((pt, p) in m.online()) {
            if (!pt.alive) continue
            val d = p.position().distanceTo(pos)
            if (d < r) p.hurt(m.level.damageSources().explosion(null, null), (m.cfg.danoMaximoC4 * (1 - d / r)).toFloat() / 5f)
        }
    }

    private fun removeDisplay() {
        display?.let { m.level.getEntity(it)?.discard() }
        display = null
    }

    fun reset() {
        removeDisplay()
        m.level.getEntities(EntityType.ITEM_DISPLAY) { it.tags.contains(TAG) }.forEach { it.discard() }
        groundC4().forEach { it.discard() }
        state = BombState.NONE
        plantedPos = null
        site = null
    }

    companion object {
        const val TAG = "shifustrike_bomba"
    }
}

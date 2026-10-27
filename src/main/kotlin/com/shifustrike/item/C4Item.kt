package com.shifustrike.item

import com.shifustrike.config.ConfigManager
import com.shifustrike.match.MatchManager
import com.shifustrike.match.Msg
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.UUID

/** Uso segurado que zera se o jogador andar (C4 e kit de desarme). */
abstract class HoldUseItem(props: Properties) : Item(props) {
    private val start = mutableMapOf<UUID, Vec3>()

    abstract fun allowed(player: Player, level: Level): Boolean
    abstract fun refusal(): String
    abstract fun seconds(): Int

    /** Ação ao completar. Devolve true se o item deve sumir da mão. */
    abstract fun complete(player: ServerPlayer): Boolean

    override fun getUseAnimation(stack: ItemStack): UseAnim = UseAnim.BOW
    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int = seconds() * 20

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (!allowed(player, level)) {
            if (player is ServerPlayer) Msg.actionBar(player, refusal())
            return InteractionResultHolder.fail(stack)
        }
        start[player.uuid] = player.position()
        player.startUsingItem(hand)
        return InteractionResultHolder.consume(stack)
    }

    override fun onUseTick(level: Level, entity: LivingEntity, stack: ItemStack, remaining: Int) {
        val s = start[entity.uuid] ?: return
        val dx = entity.x - s.x
        val dz = entity.z - s.z
        if (dx * dx + dz * dz > 0.2 * 0.2 || (entity is Player && !allowed(entity, level))) entity.stopUsingItem()
    }

    override fun releaseUsing(stack: ItemStack, level: Level, entity: LivingEntity, timeLeft: Int) {
        start.remove(entity.uuid)
    }

    override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
        start.remove(entity.uuid)
        if (level.isClientSide || entity !is ServerPlayer) return stack
        return if (complete(entity)) ItemStack.EMPTY else stack
    }
}

/** C4: segura o clique direito dentro de um bombsite para plantar. */
class C4Item(props: Properties) : HoldUseItem(props) {
    override fun allowed(player: Player, level: Level): Boolean =
        if (level.isClientSide) UseHints.canPlant(player) else MatchManager.match?.bomb?.canPlant(player as ServerPlayer) == true

    override fun refusal() = "Plante a bomba dentro de um bombsite (A ou B), parado no chão."
    override fun seconds() = ConfigManager.config.segundosPlantar
    override fun complete(player: ServerPlayer): Boolean = MatchManager.match?.bomb?.plant(player) == true
}

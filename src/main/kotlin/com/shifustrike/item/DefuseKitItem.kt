package com.shifustrike.item

import com.shifustrike.config.ConfigManager
import com.shifustrike.match.MatchManager
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

/** Kit de desarme: todo CT tem um; segura o clique direito perto da bomba plantada. */
class DefuseKitItem(props: Properties) : HoldUseItem(props) {
    override fun allowed(player: Player, level: Level): Boolean =
        if (level.isClientSide) UseHints.canDefuse(player) else MatchManager.match?.bomb?.canDefuse(player as ServerPlayer) == true

    override fun refusal() = "Chegue perto da bomba plantada para desarmar."
    override fun seconds() = ConfigManager.config.segundosDesarmar

    override fun complete(player: ServerPlayer): Boolean {
        MatchManager.match?.bomb?.defuse(player)
        return false
    }
}

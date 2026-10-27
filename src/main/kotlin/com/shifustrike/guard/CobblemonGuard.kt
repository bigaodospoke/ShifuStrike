package com.shifustrike.guard

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.shifustrike.match.MatchManager
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/** Durante a partida o jogador não solta, monta, põe no ombro nem batalha com Pokémon. */
object CobblemonGuard {
    fun register() {
        CobblemonEvents.POKEMON_SENT_PRE.subscribe(Priority.HIGH) { event ->
            val owner = event.pokemon.getOwnerPlayer() ?: return@subscribe
            if (MatchManager.isPlaying(owner)) {
                event.cancel()
                owner.displayClientMessage(Component.literal("Não dá para soltar Pokémon durante o evento.").withStyle(ChatFormatting.RED), true)
            }
        }
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGH) { event ->
            if (event.battle.players.any { MatchManager.isPlaying(it) }) event.cancel()
        }
        CobblemonEvents.RIDE_EVENT_PRE.subscribe(Priority.HIGH) { event ->
            if (MatchManager.isPlaying(event.player.uuid)) event.cancel()
        }
        CobblemonEvents.SHOULDER_MOUNT.subscribe(Priority.HIGH) { event ->
            if (MatchManager.isPlaying(event.player.uuid)) event.cancel()
        }
    }
}

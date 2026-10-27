package com.shifustrike.network

import com.shifustrike.core.BombState
import com.shifustrike.core.Team
import com.shifustrike.core.forTeam
import com.shifustrike.match.Match
import com.shifustrike.match.MatchManager
import com.shifustrike.match.Purchases
import com.shifustrike.match.Spectators
import com.shifustrike.registry.ModItems
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

object ServerNetworking {
    fun register() {
        PayloadTypeRegistry.playS2C().register(MatchInfoPayload.TYPE, MatchInfoPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(MatchStatePayload.TYPE, MatchStatePayload.CODEC)
        PayloadTypeRegistry.playS2C().register(KillFeedPayload.TYPE, KillFeedPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(RoundEndPayload.TYPE, RoundEndPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(MatchEndPayload.TYPE, MatchEndPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(MatchClearPayload.TYPE, MatchClearPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(BuyPayload.TYPE, BuyPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(RefundPayload.TYPE, RefundPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(SpectateCyclePayload.TYPE, SpectateCyclePayload.CODEC)

        // os receptores rodam no thread do servidor
        ServerPlayNetworking.registerGlobalReceiver(BuyPayload.TYPE) { p, ctx ->
            val m = running(ctx.player()) ?: return@registerGlobalReceiver
            Purchases.buy(m, ctx.player(), p.chave)
            sendState(m, ctx.player())
        }
        ServerPlayNetworking.registerGlobalReceiver(RefundPayload.TYPE) { p, ctx ->
            val m = running(ctx.player()) ?: return@registerGlobalReceiver
            Purchases.refund(m, ctx.player(), p.chave)
            sendState(m, ctx.player())
        }
        ServerPlayNetworking.registerGlobalReceiver(SpectateCyclePayload.TYPE) { _, ctx ->
            val m = running(ctx.player()) ?: return@registerGlobalReceiver
            Spectators.cycle(m, ctx.player())
        }
    }

    private fun running(p: ServerPlayer): Match? = MatchManager.match?.takeIf { MatchManager.isPlaying(p) }

    fun send(p: ServerPlayer, payload: CustomPacketPayload) {
        if (ServerPlayNetworking.canSend(p, payload.type())) ServerPlayNetworking.send(p, payload)
    }

    fun broadcast(m: Match, payload: CustomPacketPayload) = m.online().forEach { send(it.second, payload) }

    fun sendInfo(m: Match, p: ServerPlayer) {
        val pt = m.players[p.uuid] ?: return
        val a = m.arena.bombaA ?: return
        val b = m.arena.bombaB ?: return
        send(p, MatchInfoPayload(pt.team, m.shop.forTeam(pt.team).map { ShopEntryDto.from(it) }, a, b, m.cfg.raioDesarme))
    }

    fun sendState(m: Match, p: ServerPlayer) {
        val pt = m.players[p.uuid] ?: return
        val buyLeft = if (m.clock.buyOpen) m.cfg.timings().buyTicks - m.clock.ticks else 0
        send(
            p,
            MatchStatePayload(
                phase = m.clock.phase,
                round = m.round,
                ctScore = m.score.getValue(Team.CT),
                trScore = m.score.getValue(Team.TR),
                timeLeftTicks = m.clock.roundTimeLeft,
                buyTicksLeft = buyLeft.coerceAtLeast(0),
                ctAlive = m.players.values.count { it.team == Team.CT && it.alive && it.connected },
                trAlive = m.players.values.count { it.team == Team.TR && it.alive && it.connected },
                money = pt.money,
                armor = pt.armor,
                helmet = pt.helmet,
                alive = pt.alive,
                inBuyZone = Purchases.inBuyZone(m, p),
                bombPlanted = m.bomb.state == BombState.PLANTED,
                bombPos = m.bomb.plantedPos?.let { BlockPos.containing(it) },
                carryingC4 = p.inventory.countItem(ModItems.C4) > 0,
                spectating = (p.camera as? ServerPlayer)?.takeIf { it !== p }?.gameProfile?.name ?: "",
            ),
        )
    }

    fun sendStates(m: Match) = m.online().forEach { sendState(m, it.second) }
}

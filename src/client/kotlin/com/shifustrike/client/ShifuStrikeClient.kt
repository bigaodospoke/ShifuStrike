package com.shifustrike.client

import com.shifustrike.network.KillFeedPayload
import com.shifustrike.network.MatchClearPayload
import com.shifustrike.network.MatchEndPayload
import com.shifustrike.network.MatchInfoPayload
import com.shifustrike.network.MatchStatePayload
import com.shifustrike.network.RoundEndPayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object ShifuStrikeClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MatchInfoPayload.TYPE) { p, _ -> ClientMatch.onInfo(p) }
        ClientPlayNetworking.registerGlobalReceiver(MatchStatePayload.TYPE) { p, _ -> ClientMatch.onState(p) }
        ClientPlayNetworking.registerGlobalReceiver(KillFeedPayload.TYPE) { p, _ -> ClientMatch.onKill(p.info) }
        ClientPlayNetworking.registerGlobalReceiver(RoundEndPayload.TYPE) { p, _ -> ClientMatch.onRoundEnd(p) }
        ClientPlayNetworking.registerGlobalReceiver(MatchEndPayload.TYPE) { p, _ -> ClientMatch.onMatchEnd(p) }
        ClientPlayNetworking.registerGlobalReceiver(MatchClearPayload.TYPE) { _, _ -> ClientMatch.clear() }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> ClientMatch.clear() }
    }
}

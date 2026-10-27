package com.shifustrike.client

import com.mojang.blaze3d.platform.InputConstants
import com.shifustrike.client.hud.StrikeHud
import com.shifustrike.client.screen.BuyScreen
import com.shifustrike.network.KillFeedPayload
import com.shifustrike.network.SpectateCyclePayload
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import com.shifustrike.network.MatchClearPayload
import com.shifustrike.network.MatchEndPayload
import com.shifustrike.network.MatchInfoPayload
import com.shifustrike.network.MatchStatePayload
import com.shifustrike.network.RoundEndPayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

object ShifuStrikeClient : ClientModInitializer {
    val BUY_KEY: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping("key.shifustrike.comprar", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.shifustrike"),
    )

    override fun onInitializeClient() {
        HudRenderCallback.EVENT.register { g, delta -> StrikeHud.render(g, delta) }
        ClientTickEvents.END_CLIENT_TICK.register { mc ->
            // morto na partida: clique troca de colega assistido
            val s = ClientMatch.state
            if (ClientMatch.active && s != null && !s.alive && mc.screen == null) {
                while (mc.options.keyAttack.consumeClick()) ClientPlayNetworking.send(SpectateCyclePayload)
            }
            while (BUY_KEY.consumeClick()) {
                if (mc.screen != null || !ClientMatch.active) continue
                val s = ClientMatch.state!!
                when {
                    !s.alive -> mc.gui.setOverlayMessage(Component.literal("Mortos não compram."), false)
                    s.buyTicksLeft <= 0 -> mc.gui.setOverlayMessage(Component.literal("Fora do tempo de compra"), false)
                    !s.inBuyZone -> mc.gui.setOverlayMessage(Component.literal("Longe da zona de compra"), false)
                    else -> mc.setScreen(BuyScreen(BUY_KEY))
                }
            }
        }
        ClientPlayNetworking.registerGlobalReceiver(MatchInfoPayload.TYPE) { p, _ -> ClientMatch.onInfo(p) }
        ClientPlayNetworking.registerGlobalReceiver(MatchStatePayload.TYPE) { p, _ -> ClientMatch.onState(p) }
        ClientPlayNetworking.registerGlobalReceiver(KillFeedPayload.TYPE) { p, _ -> ClientMatch.onKill(p.info) }
        ClientPlayNetworking.registerGlobalReceiver(RoundEndPayload.TYPE) { p, _ -> ClientMatch.onRoundEnd(p) }
        ClientPlayNetworking.registerGlobalReceiver(MatchEndPayload.TYPE) { p, _ -> ClientMatch.onMatchEnd(p) }
        ClientPlayNetworking.registerGlobalReceiver(MatchClearPayload.TYPE) { _, _ -> ClientMatch.clear() }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> ClientMatch.clear() }
        ScreenPreview.register()
    }
}

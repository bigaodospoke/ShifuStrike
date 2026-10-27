package com.shifustrike.client

import com.shifustrike.ShifuStrike
import com.shifustrike.client.screen.BuyScreen
import com.shifustrike.core.Box
import com.shifustrike.core.DefaultShop
import com.shifustrike.core.Phase
import com.shifustrike.core.Team
import com.shifustrike.core.forTeam
import com.shifustrike.match.KillInfo
import com.shifustrike.network.MatchInfoPayload
import com.shifustrike.network.MatchStatePayload
import com.shifustrike.network.ShopEntryDto
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.screens.TitleScreen

/**
 * Só para desenvolvimento: com a variável de ambiente SHIFUSTRIKE_PREVIEW=loja|loja_ct, o cliente abre a tela de compra
 * com dados de exemplo, salva um print em run/screenshots/ e fecha. Sem a variável, não faz nada.
 */
object ScreenPreview {
    fun register() {
        val mode = System.getenv("SHIFUSTRIKE_PREVIEW") ?: return
        var ticks = -1
        ClientTickEvents.END_CLIENT_TICK.register { mc ->
            if (ticks < 0) {
                if (mc.screen !is TitleScreen) return@register
                ticks = 0
                fakeMatch(if (mode.endsWith("_ct")) Team.CT else Team.TR)
            }
            ticks++
            when (ticks) {
                40 -> mc.setScreen(BuyScreen(ShifuStrikeClient.BUY_KEY))
                80 -> Screenshot.grab(mc.gameDirectory, "preview_$mode.png", mc.mainRenderTarget) { ShifuStrike.LOGGER.info("Preview salvo: {}", it.string) }
                100 -> mc.stop()
            }
        }
    }

    private fun fakeMatch(team: Team) {
        val shop = DefaultShop.entries.forTeam(team).map { ShopEntryDto.from(it) }
        ClientMatch.onInfo(MatchInfoPayload(team, shop, Box(0, 0, 0, 1, 1, 1), Box(2, 2, 2, 3, 3, 3), 2.0))
        ClientMatch.onState(
            MatchStatePayload(
                Phase.FREEZE, 1, 0, 0, 2300, 480, 5, 5, 2900, 0, false, true, true, false, null, false, "",
            ),
        )
        ClientMatch.onKill(KillInfo("Jogador1", Team.CT, "Jogador2", Team.TR, "AK-47", true))
    }

    @Suppress("unused")
    private fun mc(): Minecraft = Minecraft.getInstance()
}

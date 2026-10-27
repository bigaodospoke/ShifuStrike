package com.shifustrike.client.hud

import com.shifustrike.client.ClientMatch
import com.shifustrike.match.KillInfo
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

/** Feed de kills no canto superior direito: no máximo 5 linhas, cada uma por 6 s. */
object KillFeed {
    private const val DURACAO_MS = 6000L

    private fun meio(k: KillInfo) = " ${if (k.weapon.isEmpty()) "morreu" else "[${k.weapon}]"}${if (k.headshot) " (HS)" else ""} "

    fun render(g: GuiGraphics, now: Long) {
        val font = Minecraft.getInstance().font
        ClientMatch.feed.removeIf { now - it.second > DURACAO_MS }
        fun largura(k: KillInfo) = font.width(k.killer ?: "") + font.width(meio(k)) + font.width(k.victim) + 8
        val maisLarga = ClientMatch.feed.maxOfOrNull { largura(it.first) } ?: return
        // em tela estreita o feed desce para não cobrir o placar do meio
        var y = if (g.guiWidth() - maisLarga - 6 < g.guiWidth() / 2 + 64) 34 else 6
        for ((k, _) in ClientMatch.feed) {
            val w = largura(k)
            val x = g.guiWidth() - w - 6
            g.fill(x, y - 2, x + w, y + 10, 0xAA000000.toInt())
            var cx = x + 4
            k.killer?.let {
                g.drawString(font, it, cx, y, ClientMatch.teamColor(k.killerTeam) or 0xFF000000.toInt(), false)
                cx += font.width(it)
            }
            g.drawString(font, meio(k), cx, y, if (k.headshot) 0xFFFF6A6A.toInt() else 0xFFE0E0E0.toInt(), false)
            cx += font.width(meio(k))
            g.drawString(font, k.victim, cx, y, ClientMatch.teamColor(k.victimTeam) or 0xFF000000.toInt(), false)
            y += 14
        }
    }
}

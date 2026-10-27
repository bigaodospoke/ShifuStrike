package com.shifustrike.client.hud

import com.shifustrike.client.ClientMatch
import com.shifustrike.core.Phase
import com.shifustrike.registry.ModItems
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

/** HUD de CS: placar e tempo no topo, dinheiro/vida/colete embaixo, feed de kills, faixas e barra de plantar/desarmar. */
object StrikeHud {
    private val C4 by lazy { ItemStack(ModItems.C4) }

    fun render(g: GuiGraphics, @Suppress("UNUSED_PARAMETER") delta: DeltaTracker?) {
        val mc = Minecraft.getInstance()
        if (mc.options.hideGui) return
        val now = ClientMatch.now()
        val s = ClientMatch.state
        if (s != null && ClientMatch.info != null) {
            top(g, s, now)
            bottomLeft(g, s)
            KillFeed.render(g, now)
            progress(g)
            spectating(g, s)
        }
        banner(g, now)
    }

    private fun time(ticks: Int): String {
        val sec = (ticks + 19) / 20
        return "${sec / 60}:${"%02d".format(sec % 60)}"
    }

    private fun scaled(g: GuiGraphics, text: String, cx: Int, y: Int, color: Int, scale: Float) {
        val font = Minecraft.getInstance().font
        g.pose().pushPose()
        g.pose().translate(cx - font.width(text) * scale / 2f, y.toFloat(), 0f)
        g.pose().scale(scale, scale, 1f)
        g.drawString(font, text, 0, 0, color, true)
        g.pose().popPose()
    }

    private fun top(g: GuiGraphics, s: com.shifustrike.network.MatchStatePayload, now: Long) {
        val cx = g.guiWidth() / 2
        g.fill(cx - 62, 2, cx + 62, 30, 0x99000000.toInt())
        // placar: CT à esquerda, TR à direita, tempo no meio
        g.fill(cx - 60, 4, cx - 30, 18, 0xFF2B4A66.toInt())
        g.fill(cx + 30, 4, cx + 60, 18, 0xFF66512B.toInt())
        scaled(g, "${s.ctScore}", cx - 45, 7, 0xFFFFFFFF.toInt(), 1f)
        scaled(g, "${s.trScore}", cx + 45, 7, 0xFFFFFFFF.toInt(), 1f)
        if (s.bombPlanted) {
            if ((now / 400) % 2 == 0L) scaled(g, "BOMBA", cx, 7, 0xFFFF4040.toInt(), 1f)
        } else {
            scaled(g, time(s.timeLeftTicks), cx, 7, if (s.phase == Phase.FREEZE) 0xFFAAAAAA.toInt() else 0xFFFFFFFF.toInt(), 1f)
        }
        scaled(g, "${s.ctAlive} vivos", cx - 45, 21, ClientMatch.COR_CT or 0xFF000000.toInt(), 0.6f)
        scaled(g, "${s.trAlive} vivos", cx + 45, 21, ClientMatch.COR_TR or 0xFF000000.toInt(), 0.6f)
        if (s.buyTicksLeft > 0 && s.alive) scaled(g, "Compra: ${time(s.buyTicksLeft)} [B]", cx, 21, 0xFF9FE08F.toInt(), 0.6f)
        else if (s.phase == Phase.FREEZE) scaled(g, "Congelado", cx, 21, 0xFFAAAAAA.toInt(), 0.6f)
    }

    private fun bottomLeft(g: GuiGraphics, s: com.shifustrike.network.MatchStatePayload) {
        val mc = Minecraft.getInstance()
        val font = mc.font
        val h = g.guiHeight()
        val vida = if (s.alive) ((mc.player?.health ?: 20f) * 5).toInt().coerceIn(0, 100) else 0
        val x = 6
        val y = h - 40
        g.fill(x - 2, y - 2, x + 98, y + 34, 0x99000000.toInt())
        g.drawString(font, "$${s.money}", x + 2, y + 1, 0xFF9FE08F.toInt(), true)
        if (s.carryingC4) g.renderItem(C4, x + 78, y - 3)
        g.drawString(font, "Vida $vida", x + 2, y + 12, if (vida <= 25) 0xFFFF6A6A.toInt() else 0xFFFFFFFF.toInt(), true)
        val colete = if (s.armor > 0) "Colete ${s.armor}${if (s.helmet) " + capacete" else ""}" else "Sem colete"
        val esc = minOf(1f, 94f / font.width(colete))
        g.pose().pushPose()
        g.pose().translate((x + 2).toFloat(), (y + 23).toFloat(), 0f)
        g.pose().scale(esc, esc, 1f)
        g.drawString(font, colete, 0, 0, 0xFFC9CDD2.toInt(), true)
        g.pose().popPose()
    }

    private fun progress(g: GuiGraphics) {
        val p = Minecraft.getInstance().player ?: return
        if (!p.isUsingItem) return
        val stack = p.useItem
        val label = when {
            stack.`is`(ModItems.C4) -> "Plantando..."
            stack.`is`(ModItems.KIT) -> "Desarmando..."
            else -> return
        }
        val total = stack.getUseDuration(p).coerceAtLeast(1)
        val frac = (p.ticksUsingItem.toFloat() / total).coerceIn(0f, 1f)
        val cx = g.guiWidth() / 2
        val y = g.guiHeight() / 2 + 20
        g.fill(cx - 60, y, cx + 60, y + 6, 0xAA000000.toInt())
        g.fill(cx - 59, y + 1, cx - 59 + (118 * frac).toInt(), y + 5, 0xFFE8B64C.toInt())
        scaled(g, label, cx, y - 10, 0xFFFFFFFF.toInt(), 1f)
    }

    private fun spectating(g: GuiGraphics, s: com.shifustrike.network.MatchStatePayload) {
        if (s.alive) return
        val cx = g.guiWidth() / 2
        val y = g.guiHeight() - 34
        val quem = if (s.spectating.isEmpty()) "Você morreu" else "Assistindo: ${s.spectating}"
        scaled(g, quem, cx, y, 0xFFFFFFFF.toInt(), 1f)
        scaled(g, "[Clique] próximo · volta na próxima rodada", cx, y + 11, 0xFFAAAAAA.toInt(), 0.7f)
    }

    private fun banner(g: GuiGraphics, now: Long) {
        val b = ClientMatch.banner ?: return
        if (now > b.until) return
        val cx = g.guiWidth() / 2
        val y = g.guiHeight() / 3
        val linhas = b.lines.take(10)
        val altura = 30 + if (linhas.isEmpty()) 0 else 6 + linhas.size * 10
        g.fill(0, y - 6, g.guiWidth(), y + altura, 0xAA000000.toInt())
        scaled(g, b.title, cx, y, b.color or 0xFF000000.toInt(), 2f)
        if (b.subtitle.isNotEmpty()) scaled(g, b.subtitle, cx, y + 19, 0xFFE0E0E0.toInt(), 1f)
        var ly = y + 34
        for (l in linhas) {
            scaled(g, "${l.name}   ${l.kills} / ${l.deaths}", cx, ly, ClientMatch.teamColor(l.team) or 0xFF000000.toInt(), 0.8f)
            ly += 10
        }
    }
}

package com.shifustrike.client.screen

import com.mojang.blaze3d.systems.RenderSystem
import com.shifustrike.client.ClientMatch
import com.shifustrike.core.EntryKind
import com.shifustrike.core.Team
import com.shifustrike.network.BuyPayload
import com.shifustrike.network.RefundPayload
import com.shifustrike.network.ShopEntryDto
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/** Tela de compra no layout aprovado (mockup tela-compra.html): 5 colunas numeradas, silhuetas na cor do time. */
class BuyScreen(private val openKey: KeyMapping) : Screen(Component.literal("Comprar")) {
    private val colunas = listOf("Equipamento", "Pistolas", "Intermediárias", "Rifles", "Granadas")

    /** Coluna escolhida pelo teclado (1-5), ou -1. */
    private var colunaTeclado = -1

    private data class Card(val e: ShopEntryDto, val x: Int, val y: Int, val w: Int, val h: Int) {
        fun hit(mx: Double, my: Double) = mx >= x && mx < x + w && my >= y && my < y + h
    }

    private var cards: List<Card> = emptyList()

    override fun isPauseScreen() = false

    override fun renderBackground(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // sem o desfoque padrão: o jogo continua visível atrás, como no CS
    }

    override fun tick() {
        val s = ClientMatch.state
        if (s == null || !s.alive || s.buyTicksLeft <= 0) onClose()
    }

    private fun price(e: ShopEntryDto): Int {
        val s = ClientMatch.state ?: return e.preco
        return if (e.tipo == EntryKind.COLETE_CAPACETE && s.armor >= 100 && !s.helmet) e.precoAlt ?: e.preco else e.preco
    }

    private fun time(ticks: Int): String {
        val s = (ticks + 19) / 20
        return "${s / 60}:${"%02d".format(s % 60)}"
    }

    /** Texto que encolhe para caber em `maxW` (nunca maior que `maxScale`). */
    private fun fit(g: GuiGraphics, text: String, x: Int, y: Int, maxW: Int, color: Int, maxScale: Float, right: Boolean = false) {
        val scale = minOf(maxScale, maxW.toFloat() / font.width(text).coerceAtLeast(1))
        small(g, text, x, y, color, scale, right)
    }

    private fun small(g: GuiGraphics, text: String, x: Int, y: Int, color: Int, scale: Float = 0.7f, right: Boolean = false) {
        g.pose().pushPose()
        val w = font.width(text) * scale
        g.pose().translate(if (right) x - w else x.toFloat(), y.toFloat(), 0f)
        g.pose().scale(scale, scale, 1f)
        g.drawString(font, text, 0, 0, color, false)
        g.pose().popPose()
    }

    override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val info = ClientMatch.info ?: return
        val s = ClientMatch.state ?: return
        val cor = ClientMatch.teamColor(info.team)

        val px = 8
        val py = 8
        val pw = (width * 0.62).toInt().coerceAtLeast(300).coerceAtMost(width - 16)
        val ph = height - 16
        g.fill(px, py, px + pw, py + ph, 0xDD15181B.toInt())
        g.renderOutline(px, py, pw, ph, 0xFF000000.toInt())

        // topo: dinheiro | tempo de compra | time e rodada (cada um no seu terço)
        val terco = (pw - 12) / 3
        g.drawString(font, "$${s.money}", px + 6, py + 6, 0xFF9FE08F.toInt(), false)
        val tempo = "Tempo de compra restante: ${time(s.buyTicksLeft)}"
        val esTempo = minOf(1f, (terco + 20f) / font.width(tempo))
        small(g, tempo, px + (pw - (font.width(tempo) * esTempo).toInt()) / 2, py + 6, 0xFFC9CDD2.toInt(), esTempo)
        val quem = "${if (info.team == Team.CT) "Antiterroristas" else "Terroristas"} · Rodada ${s.round}"
        fit(g, quem, px + pw - 6, py + 7, terco - 14, 0xFF7D848C.toInt(), 0.8f, right = true)
        g.fill(px + 4, py + 18, px + pw - 4, py + 19, 0xFF3A4047.toInt())

        // colunas
        val pad = 6
        val gap = 6
        val colW = (pw - pad * 2 - gap * 4) / 5
        val topCards = py + 36
        val footerH = 18
        val cardGap = 5
        val cardH = ((py + ph - footerH - 4 - topCards - cardGap * 4) / 5).coerceIn(24, 54)
        val novas = mutableListOf<Card>()
        for (c in 0 until 5) {
            val cx = px + pad + c * (colW + gap)
            val sel = colunaTeclado == c
            g.drawString(font, "${c + 1}", cx, py + 23, 0xFF7D848C.toInt(), false)
            fit(g, colunas[c], cx + 9, py + 23, colW - 9, if (sel) cor or 0xFF000000.toInt() else 0xFFFFFFFF.toInt(), 1f)
            for (e in info.shop.filter { it.coluna == c + 1 }) {
                val cy = topCards + (e.posicao - 1) * (cardH + cardGap)
                novas += Card(e, cx, cy, colW, cardH)
            }
        }
        cards = novas

        for (card in cards) drawCard(g, card, cor, s.money, mouseX, mouseY)

        // rodapé
        val fy = py + ph - footerH
        g.fill(px + 4, fy, px + pw - 4, fy + 1, 0xFF3A4047.toInt())
        g.drawString(font, "$${s.money}", px + 6, fy + 6, 0xFF9FE08F.toInt(), false)
        small(g, "[B] FECHAR · [1-5] CATEGORIA / ITEM · [CLIQUE DIREITO] REEMBOLSAR", px + pw - 6, fy + 7, 0xFF7D848C.toInt(), 0.7f, right = true)

        // modelo do jogador à direita
        val player = minecraft?.player
        if (player != null) {
            val x1 = px + pw + 10
            val x2 = width - 10
            if (x2 - x1 > 40) {
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, py + 20, x2, height - 20, (height / 4).coerceAtMost(90), 0.0625f, mouseX.toFloat(), mouseY.toFloat(), player)
            }
        }
        super.render(g, mouseX, mouseY, partialTick)
    }

    private fun drawCard(g: GuiGraphics, c: Card, cor: Int, money: Int, mouseX: Int, mouseY: Int) {
        val e = c.e
        val emBreve = e.tipo == EntryKind.EM_BREVE
        val preco = price(e)
        val caro = !emBreve && preco > money
        val hover = !emBreve && c.hit(mouseX.toDouble(), mouseY.toDouble())
        g.fill(c.x, c.y, c.x + c.w, c.y + c.h, if (hover) 0xFF2E3338.toInt() else 0xFF262A2F.toInt())
        if (hover) g.renderOutline(c.x, c.y, c.w, c.h, 0xFFCFD3D8.toInt())

        val alpha = if (emBreve) 0.35f else if (caro) 0.55f else 1f
        val textCol = if (emBreve) 0x59D7DBE0 else 0xFFD7DBE0.toInt()
        small(g, "${e.posicao}", c.x + 3, c.y + 3, if (emBreve) 0x597D848C else 0xFF7D848C.toInt())
        fit(g, e.nome, c.x + c.w - 3, c.y + 3, c.w - 12, textCol, 0.65f, right = true)
        small(g, if (emBreve) "em breve" else "$$preco", c.x + c.w - 3, c.y + c.h - 8, if (caro) 0xFFE06A6A.toInt() else textCol, 0.7f, right = true)

        val icon = IconCache.icon(e.icone) ?: return
        val size = (c.h - 14).coerceAtMost(c.w - 10).coerceAtLeast(8)
        val ix = c.x + (c.w - size) / 2
        val iy = c.y + 8 + (c.h - 14 - size) / 2
        RenderSystem.enableBlend()
        RenderSystem.setShaderColor(((cor shr 16) and 0xFF) / 255f, ((cor shr 8) and 0xFF) / 255f, (cor and 0xFF) / 255f, alpha)
        g.blit(icon, ix, iy, size, size, 0f, 0f, 64, 64, 64, 64)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.disableBlend()
    }

    private fun buy(e: ShopEntryDto) {
        if (e.tipo != EntryKind.EM_BREVE) ClientPlayNetworking.send(BuyPayload(e.chave))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val card = cards.firstOrNull { it.hit(mouseX, mouseY) } ?: return super.mouseClicked(mouseX, mouseY, button)
        when (button) {
            GLFW.GLFW_MOUSE_BUTTON_LEFT -> buy(card.e)
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> ClientPlayNetworking.send(RefundPayload(card.e.chave))
        }
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && colunaTeclado >= 0) {
            colunaTeclado = -1
            return true
        }
        if (openKey.matches(keyCode, scanCode)) {
            onClose()
            return true
        }
        val n = keyCode - GLFW.GLFW_KEY_1
        if (n in 0..4) {
            if (colunaTeclado < 0) {
                colunaTeclado = n
            } else {
                ClientMatch.info?.shop?.firstOrNull { it.coluna == colunaTeclado + 1 && it.posicao == n + 1 }?.let { buy(it) }
                colunaTeclado = -1
            }
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}

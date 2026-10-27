package com.shifustrike.match

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.server.level.ServerPlayer

/** Textos do chat e títulos do evento. */
object Msg {
    fun prefix(): MutableComponent = Component.literal("[CS] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)

    fun info(text: String): Component = prefix().append(Component.literal(text).withStyle(ChatFormatting.YELLOW))
    fun error(text: String): Component = prefix().append(Component.literal(text).withStyle(ChatFormatting.RED))

    fun send(player: ServerPlayer, text: String) = player.sendSystemMessage(info(text))
    fun sendError(player: ServerPlayer, text: String) = player.sendSystemMessage(error(text))
    fun actionBar(player: ServerPlayer, text: String) = player.displayClientMessage(Component.literal(text), true)

    fun broadcast(players: Iterable<ServerPlayer>, text: String) {
        val c = info(text)
        players.forEach { it.sendSystemMessage(c) }
    }

    fun title(player: ServerPlayer, title: Component, subtitle: Component? = null, fadeIn: Int = 0, stay: Int = 40, fadeOut: Int = 10) {
        player.connection.send(ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut))
        player.connection.send(ClientboundSetSubtitleTextPacket(subtitle ?: Component.empty()))
        player.connection.send(ClientboundSetTitleTextPacket(title))
    }
}

package com.shifustrike.guard

import com.shifustrike.match.MatchManager
import com.shifustrike.match.Msg
import net.minecraft.commands.CommandSourceStack

/** Durante a partida o jogador só usa os comandos da lista (padrão: /cs e mensagens). */
object CommandGuard {
    fun isAllowed(commandLine: String, allowed: List<String>): Boolean {
        val first = commandLine.trim().removePrefix("/").substringBefore(' ').lowercase()
        if (first.isEmpty()) return false
        return first.substringAfter(':') in allowed
    }

    /** Chamado pelo mixin antes de qualquer comando. Devolve false se o comando deve ser barrado. */
    fun check(source: CommandSourceStack, commandLine: String): Boolean {
        val p = source.player ?: return true
        val m = MatchManager.match ?: return true
        if (!MatchManager.isPlaying(p)) return true
        if (isAllowed(commandLine, m.cfg.comandosPermitidos)) return true
        Msg.sendError(p, "Durante o evento só dá para usar /cs e mensagens. Use /cs sair para sair.")
        return false
    }
}

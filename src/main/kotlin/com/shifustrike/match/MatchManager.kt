package com.shifustrike.match

import com.shifustrike.arena.ArenaData
import com.shifustrike.arena.ArenaStorage
import com.shifustrike.config.ConfigManager
import com.shifustrike.config.StrikeConfig
import com.shifustrike.core.ShopEntry
import com.shifustrike.data.SnapshotStorage
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.UUID
import kotlin.random.Random

/** Só existe uma partida por vez. Todas as funções de ação devolvem a mensagem de erro, ou null se deu certo. */
object MatchManager {
    var match: Match? = null
        private set

    fun create(server: MinecraftServer): String? {
        if (match != null) return "Já existe uma partida. Use /cs cancelar antes de criar outra."
        val arena = ArenaStorage.arena
        val problems = arena.problemas()
        if (problems.isNotEmpty()) return "O mapa não está pronto: ${problems.joinToString("; ")}"
        val shop = ConfigManager.activeShop()
        if (shop.none { it.item != null }) return "Nenhuma arma na loja (o PointBlank está instalado?)."
        match = Match(server, arena, ConfigManager.config, shop, Random.Default)
        return null
    }

    /** Porta dos GameTests: partida com mapa e config próprios. */
    fun createForTest(server: MinecraftServer, arena: ArenaData, cfg: StrikeConfig, shop: List<ShopEntry>, random: Random = Random(7)): Match {
        match?.end(null)
        return Match(server, arena, cfg, shop, random).also { match = it }
    }

    fun join(p: ServerPlayer): String? {
        val m = match ?: return "Nenhuma partida aberta. Espere a staff usar /cs criar."
        if (m.state != Match.State.LOBBY) return "A partida já começou."
        if (!m.lobby.add(p.uuid)) return "Você já está inscrito."
        return null
    }

    fun leave(p: ServerPlayer): String? {
        val m = match ?: return "Nenhuma partida aberta."
        return when (m.state) {
            Match.State.LOBBY -> if (m.lobby.remove(p.uuid)) null else "Você não está inscrito."
            Match.State.RUNNING -> if (p.uuid in m.players) { m.leave(p); null } else "Você não está na partida."
            Match.State.ENDED -> "A partida já acabou."
        }
    }

    fun start(server: MinecraftServer): String? {
        val m = match ?: return "Nenhuma partida aberta. Use /cs criar."
        return m.begin()
    }

    fun cancel(server: MinecraftServer): String? {
        val m = match ?: return "Nenhuma partida aberta."
        m.end(null)
        return null
    }

    fun participant(p: ServerPlayer): Participant? = participant(p.uuid)

    fun participant(id: UUID): Participant? = match?.takeIf { it.state == Match.State.RUNNING }?.players?.get(id)

    /** Dois jogadores vivos da partida, de times diferentes. */
    fun areOpponents(a: ServerPlayer, b: ServerPlayer): Boolean {
        val pa = participant(a) ?: return false
        val pb = participant(b) ?: return false
        return pa.alive && pb.alive && pa.team != pb.team
    }

    fun isPlaying(p: ServerPlayer): Boolean = participant(p.uuid) != null
    fun isPlaying(id: UUID): Boolean = participant(id) != null

    internal fun clear(m: Match) {
        if (match === m) match = null
    }

    fun tick(server: MinecraftServer) {
        match?.tick()
    }

    fun onJoin(p: ServerPlayer) {
        val pt = participant(p)
        if (pt != null) {
            // volta no meio da partida: assiste até a próxima rodada
            pt.connected = true
            pt.alive = false
            p.inventory.clearContent()
            Spectators.makeSpectator(match!!, p)
            return
        }
        val arena = match?.arena ?: ArenaStorage.arena
        val level = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(arena.dimensao)))
        SnapshotStorage.restoreOnJoin(p, arena.saida, level)
    }

    fun onDisconnect(p: ServerPlayer) {
        val pt = participant(p) ?: return
        Deaths.removeFromRound(match!!, p, pt)
        pt.connected = false
    }

    fun shutdown(server: MinecraftServer) {
        match?.end(null)
    }
}

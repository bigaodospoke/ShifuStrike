package com.shifustrike.match

import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.util.party
import com.shifustrike.arena.ArenaData
import com.shifustrike.arena.Ponto
import com.shifustrike.config.StrikeConfig
import com.shifustrike.core.RoundClock
import com.shifustrike.core.ShopEntry
import com.shifustrike.core.Team
import com.shifustrike.core.TeamBalancer
import com.shifustrike.data.PlayerSnapshot
import com.shifustrike.data.SnapshotStorage
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.GameType
import java.util.UUID
import kotlin.random.Random

/** Uma partida: inscrições (LOBBY), rodadas (RUNNING) e fim (ENDED). */
class Match(
    val server: MinecraftServer,
    val arena: ArenaData,
    val cfg: StrikeConfig,
    val shop: List<ShopEntry>,
    val random: Random,
) {
    enum class State { LOBBY, RUNNING, ENDED }

    var state = State.LOBBY
        private set
    val lobby = LinkedHashSet<UUID>()
    val players = LinkedHashMap<UUID, Participant>()
    val score = mutableMapOf(Team.CT to 0, Team.TR to 0)
    var round = 0
        private set
    val clock = RoundClock(cfg.timings())

    /** Tamanho de cada time no início da partida (para a regra do time que ficou vazio). */
    var startedTeamSizes: Map<Team, Int> = emptyMap()
        private set

    val level: ServerLevel
        get() = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(arena.dimensao))) ?: server.overworld()

    fun player(id: UUID): ServerPlayer? = server.playerList.getPlayer(id)

    /** Participantes conectados com o jogador online. */
    fun online(): List<Pair<Participant, ServerPlayer>> = players.values.mapNotNull { pt -> player(pt.id)?.let { pt to it } }

    fun broadcast(text: String) = Msg.broadcast(online().map { it.second }, text)

    /** Inicia a partida. Devolve a mensagem de erro, ou null se começou. */
    fun begin(): String? {
        if (state != State.LOBBY) return "A partida já começou."
        val problems = arena.problemas()
        if (problems.isNotEmpty()) return "O mapa não está pronto: ${problems.joinToString("; ")}"
        lobby.removeIf { player(it) == null }
        val emBatalha = lobby.mapNotNull { player(it) }.filter { BattleRegistry.getBattleByParticipatingPlayer(it) != null }
        emBatalha.forEach {
            lobby.remove(it.uuid)
            Msg.sendError(it, "Você saiu do evento porque está numa batalha Pokémon.")
        }
        if (lobby.size < cfg.minimoJogadores) return "Jogadores insuficientes: mínimo de ${cfg.minimoJogadores}, inscritos ${lobby.size}."

        val teams = TeamBalancer.split(lobby.toList(), random)
        for (id in lobby) {
            val p = player(id) ?: continue
            SnapshotStorage.save(id, PlayerSnapshot.capture(p))
            p.stopUsingItem()
            p.inventory.clearContent()
            recallPokemon(p)
            players[id] = Participant(id, p.gameProfile.name, teams.getValue(id)).also { it.money = cfg.dinheiroInicial }
        }
        startedTeamSizes = Team.entries.associateWith { t -> players.values.count { it.team == t } }
        state = State.RUNNING
        startRound()
        broadcast("A partida começou! Primeiro time a ${cfg.vitoriasParaVencer} vitórias vence.")
        return null
    }

    private fun recallPokemon(p: ServerPlayer) {
        try {
            p.party().forEach { if (it.entity != null) it.recall() }
        } catch (e: Exception) {
            com.shifustrike.ShifuStrike.LOGGER.warn("Não deu para recolher os Pokémon de {}: {}", p.gameProfile.name, e.message)
        }
    }

    private fun spawnsOf(team: Team): List<Ponto> = if (team == Team.CT) arena.spawnsCt else arena.spawnsTr

    fun teleportToSpawn(p: ServerPlayer, team: Team, index: Int) {
        val spawns = spawnsOf(team)
        val s = spawns[index % spawns.size]
        p.teleportTo(level, s.x, s.y, s.z, s.yaw, s.pitch)
    }

    fun startRound() {
        round++
        clock.startRound()
        for (team in Team.entries) {
            online().filter { it.first.team == team }.shuffled(random).forEachIndexed { i, (pt, p) ->
                teleportToSpawn(p, team, i)
                p.setGameMode(GameType.ADVENTURE)
                pt.alive = true
            }
        }
    }

    fun tick() {
        if (state != State.RUNNING) return
        clock.tick()
    }

    /** Tira um jogador da partida em andamento e devolve as coisas dele. */
    fun leave(p: ServerPlayer) {
        players.remove(p.uuid) ?: return
        restore(p)
    }

    private fun restore(p: ServerPlayer) {
        val s = SnapshotStorage.load(p.uuid) ?: return
        s.restore(p, arena.saida, level)
        SnapshotStorage.delete(p.uuid)
    }

    /** Fim da partida (vencedor null = cancelada). Quem está offline recebe tudo de volta ao entrar. */
    fun end(winner: Team?) {
        if (state == State.ENDED) return
        val wasRunning = state == State.RUNNING
        state = State.ENDED
        clock.endMatch()
        if (wasRunning) {
            val online = online()
            val msg = when (winner) {
                null -> "A partida foi cancelada."
                Team.CT -> "Os Antiterroristas venceram a partida! (${score[Team.CT]} × ${score[Team.TR]})"
                Team.TR -> "Os Terroristas venceram a partida! (${score[Team.TR]} × ${score[Team.CT]})"
            }
            Msg.broadcast(online.map { it.second }, msg)
            online.forEach { (_, p) -> restore(p) }
        }
        MatchManager.clear(this)
    }
}

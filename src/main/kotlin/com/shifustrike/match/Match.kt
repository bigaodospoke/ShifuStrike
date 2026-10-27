package com.shifustrike.match

import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.util.party
import com.shifustrike.arena.ArenaData
import com.shifustrike.arena.Ponto
import com.shifustrike.config.StrikeConfig
import com.shifustrike.core.BombState
import com.shifustrike.core.Economy
import com.shifustrike.core.Phase
import com.shifustrike.core.RoundClock
import com.shifustrike.core.RoundOutcome
import com.shifustrike.core.RoundRules
import com.shifustrike.core.RoundSnapshot
import com.shifustrike.core.ShopEntry
import com.shifustrike.core.Team
import com.shifustrike.core.TeamBalancer
import com.shifustrike.core.WinReason
import com.shifustrike.data.PlayerSnapshot
import com.shifustrike.data.SnapshotStorage
import com.shifustrike.data.applyFreeze
import com.shifustrike.data.removeFreeze
import com.shifustrike.pointblank.EventItems
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.AABB
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
        // time que tinha gente e ficou sem ninguém conectado perde a partida
        for (team in Team.entries) {
            if ((startedTeamSizes[team] ?: 0) > 0 && online().none { it.first.team == team }) {
                broadcast("Os ${teamName(team)} ficaram sem jogadores.")
                end(team.other())
                return
            }
        }
        round++
        clock.startRound()
        bombWasPlanted = false
        bomb.reset()
        clearDroppedItems()
        players.values.filter { !it.connected }.forEach { it.alive = false }
        for (team in Team.entries) {
            online().filter { it.first.team == team }.shuffled(random).forEachIndexed { i, (pt, p) ->
                val fresh = !pt.alive
                p.stopUsingItem()
                teleportToSpawn(p, team, i)
                p.setGameMode(GameType.ADVENTURE)
                p.setCamera(p)
                p.health = p.maxHealth
                p.removeAllEffects()
                p.clearFire()
                p.fallDistance = 0f
                applyFreeze(p)
                Loadouts.giveRoundBasics(p, team, shop, fresh)
                if (fresh) {
                    pt.armor = 0
                    pt.helmet = false
                }
                pt.alive = true
                pt.purchases.clear()
            }
        }
        roundStart = Team.entries.associateWith { t -> players.values.count { it.team == t && it.alive } }
        bomb.giveToRandomTr()
        broadcast("Rodada $round — CT ${score[Team.CT]} × ${score[Team.TR]} TR")
    }

    /** Vivos de cada time quando a rodada começou (time vazio não perde por eliminação). */
    private var roundStart: Map<Team, Int> = emptyMap()

    /** Se a C4 foi plantada nesta rodada (bônus do TR que perde). Preenchido pela bomba. */
    var bombWasPlanted = false

    val lossStreak = mutableMapOf(Team.CT to 0, Team.TR to 0)

    private fun alive(team: Team) = players.values.count { it.team == team && it.alive && it.connected }

    fun snapshot(bomb: BombState = BombState.NONE) = RoundSnapshot(
        ctAlive = alive(Team.CT),
        trAlive = alive(Team.TR),
        ctStarted = roundStart[Team.CT] ?: 0,
        trStarted = roundStart[Team.TR] ?: 0,
        bomb = bomb,
        timeOver = clock.roundTimeOver,
    )

    val bomb = Bomb(this)

    /** Resultado da última rodada (para mensagens e testes). */
    var lastOutcome: RoundOutcome? = null
        private set

    fun tick() {
        if (state != State.RUNNING) return
        val wasFrozen = clock.frozen
        clock.tick()
        if (wasFrozen && !clock.frozen) online().forEach { removeFreeze(it.second) }
        for ((pt, p) in online()) {
            if (!pt.alive) continue
            // fome abaixo de 18 não regenera vida; acima de 6 ainda deixa correr
            p.foodData.foodLevel = 17
            p.foodData.setSaturation(0f)
            if (clock.ticks % 10 == 0) Loadouts.normalize(p, shop)
        }
        Spectators.tick(this)
        bomb.tick()
        when (clock.phase) {
            Phase.LIVE, Phase.PLANTED -> RoundRules.evaluate(snapshot(bomb.state))?.let { endRound(it) }
            Phase.ROUND_END -> if (clock.roundEndDone) {
                val winner = RoundRules.matchWinner(score.getValue(Team.CT), score.getValue(Team.TR), cfg.vitoriasParaVencer)
                if (winner != null) end(winner) else startRound()
            }
            else -> {}
        }
    }

    fun endRound(o: RoundOutcome) {
        if (clock.phase == Phase.ROUND_END || clock.phase == Phase.MATCH_END) return
        clock.endRound()
        lastOutcome = o
        score[o.winner] = score.getValue(o.winner) + 1
        val loser = o.winner.other()
        lossStreak[loser] = lossStreak.getValue(loser) + 1
        lossStreak[o.winner] = 0
        val rules = cfg.economy()
        val pay = Economy.roundPayout(rules, o, lossStreak.getValue(loser), bombWasPlanted)
        players.values.forEach { it.money = Economy.add(rules, it.money, pay.getValue(it.team)) }
        val title = Component.literal("${teamName(o.winner)} venceram").withStyle(if (o.winner == Team.CT) ChatFormatting.AQUA else ChatFormatting.GOLD)
        val sub = Component.literal(reasonText(o.reason))
        online().forEach { (_, p) -> Msg.title(p, title, sub, stay = 60) }
        onRoundEnd(o)
    }

    /** Avisa a rede (Task 16) do fim da rodada. */
    var onRoundEnd: (RoundOutcome) -> Unit = {}

    /** Avisa a rede (Task 16) de cada morte, para o feed de kills. */
    var onKill: (KillInfo) -> Unit = {}

    private fun reasonText(r: WinReason) = when (r) {
        WinReason.ELIMINATION -> "Eliminação"
        WinReason.TIME -> "Tempo esgotado"
        WinReason.BOMB_EXPLODED -> "A bomba explodiu"
        WinReason.BOMB_DEFUSED -> "A bomba foi desarmada"
        WinReason.FORFEIT -> "O outro time ficou sem jogadores"
    }

    fun teamName(t: Team) = if (t == Team.CT) "Antiterroristas" else "Terroristas"

    /** Itens do evento largados no chão da área somem. */
    fun clearDroppedItems() {
        val box = arena.area ?: return
        val aabb = AABB(box.minX.toDouble(), box.minY.toDouble(), box.minZ.toDouble(), box.maxX + 1.0, box.maxY + 1.0, box.maxZ + 1.0)
        level.getEntitiesOfClass(ItemEntity::class.java, aabb) { EventItems.isEvent(it.item) }.forEach { it.discard() }
    }

    /** Tira um jogador da partida em andamento e devolve as coisas dele. */
    fun leave(p: ServerPlayer) {
        val pt = players[p.uuid] ?: return
        Deaths.removeFromRound(this, p, pt)
        players.remove(p.uuid)
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
            bomb.reset()
            clearDroppedItems()
        }
        MatchManager.clear(this)
    }
}

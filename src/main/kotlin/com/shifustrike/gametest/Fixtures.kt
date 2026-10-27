package com.shifustrike.gametest

import com.shifustrike.arena.ArenaData
import com.shifustrike.arena.Ponto
import com.shifustrike.config.ConfigManager
import com.shifustrike.config.StrikeConfig
import com.shifustrike.core.Box
import com.shifustrike.match.Match
import com.shifustrike.match.MatchManager
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3

/** Mapa e config de teste em volta do lugar do GameTest. Só existe uma partida por vez: cada teste roda num lote próprio. */
object Fixtures {
    fun arena(h: GameTestHelper): ArenaData {
        fun p(x: Double, y: Double, z: Double): Ponto {
            val v = h.absoluteVec(Vec3(x, y, z))
            return Ponto(v.x, v.y, v.z, 0f, 0f)
        }
        val o = h.absolutePos(net.minecraft.core.BlockPos.ZERO)
        return ArenaData().apply {
            dimensao = h.level.dimension().location().toString()
            spawnsCt += p(1.5, 2.0, 1.5)
            spawnsTr += p(7.5, 2.0, 1.5)
            bombaA = Box.of(o.x + 2, o.y + 1, o.z + 5, o.x + 4, o.y + 4, o.z + 7)
            bombaB = Box.of(o.x + 6, o.y + 1, o.z + 5, o.x + 8, o.y + 4, o.z + 7)
            area = Box.of(o.x - 4, o.y - 2, o.z - 4, o.x + 12, o.y + 10, o.z + 12)
            saida = p(4.5, 2.0, 30.5)
        }
    }

    fun config(change: StrikeConfig.() -> Unit = {}) = StrikeConfig().apply(change).also { it.sanitize() }

    /** Cancela qualquer partida que tenha sobrado de outro teste. */
    fun reset(h: GameTestHelper) {
        if (MatchManager.match != null) MatchManager.cancel(h.level.server)
    }

    fun match(h: GameTestHelper, cfg: StrikeConfig = config()): Match {
        reset(h)
        return MatchManager.createForTest(h.level.server, arena(h), cfg, ConfigManager.activeShop())
    }

    @Suppress("DEPRECATION")
    fun player(h: GameTestHelper, withDiamond: Boolean = true): ServerPlayer {
        val p = h.makeMockServerPlayerInLevel()
        p.setGameMode(GameType.SURVIVAL)
        if (withDiamond) p.inventory.add(ItemStack(Items.DIAMOND))
        p.teleportTo(h.level, h.absoluteVec(Vec3(4.5, 2.0, 20.5)).x, h.absoluteVec(Vec3(4.5, 2.0, 20.5)).y, h.absoluteVec(Vec3(4.5, 2.0, 20.5)).z, 0f, 0f)
        return p
    }

    /** Dois jogadores na partida já iniciada; devolve (CT, TR). */
    fun started(h: GameTestHelper, cfg: StrikeConfig = config()): Triple<Match, ServerPlayer, ServerPlayer> {
        val m = match(h, cfg)
        val a = player(h)
        val b = player(h)
        MatchManager.join(a)
        MatchManager.join(b)
        val err = MatchManager.start(h.level.server)
        if (err != null) throw IllegalStateException("start falhou: $err")
        val ct = if (m.players.getValue(a.uuid).team == com.shifustrike.core.Team.CT) a else b
        val tr = if (ct === a) b else a
        return Triple(m, ct, tr)
    }

    /** `n` jogadores na partida já iniciada. */
    fun startedN(h: GameTestHelper, n: Int, cfg: StrikeConfig = config()): Pair<Match, List<ServerPlayer>> {
        val m = match(h, cfg)
        val ps = (1..n).map { player(h) }
        ps.forEach { MatchManager.join(it) }
        val err = MatchManager.start(h.level.server)
        if (err != null) throw IllegalStateException("start falhou: $err")
        return m to ps
    }

    /** Põe o jogador no meio do bombsite A, "no chão" (jogador falso não manda pacote de movimento). */
    fun noSiteA(h: GameTestHelper, p: ServerPlayer) {
        val v = h.absoluteVec(Vec3(3.5, 2.0, 6.5))
        p.teleportTo(h.level, v.x, v.y, v.z, 0f, 0f)
        p.setOnGround(true)
    }

    fun modo(p: ServerPlayer): GameType = p.gameMode.gameModeForPlayer
}

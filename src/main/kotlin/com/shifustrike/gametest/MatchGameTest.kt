package com.shifustrike.gametest

import com.shifustrike.core.Team
import com.shifustrike.data.SnapshotStorage
import com.shifustrike.gametest.Fixtures.modo
import com.shifustrike.match.Match
import com.shifustrike.match.MatchManager
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3

class MatchGameTest : FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "partida_inicia")
    fun inicia_divide_e_teleporta(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h)
        val arena = m.arena
        h.assertTrue(m.state == Match.State.RUNNING, "estado=${m.state}")
        h.assertTrue(m.players.getValue(ct.uuid).team == Team.CT && m.players.getValue(tr.uuid).team == Team.TR, "times")
        for (p in listOf(ct, tr)) {
            h.assertTrue(modo(p) == GameType.ADVENTURE, "modo=${modo(p)}")
            h.assertTrue(p.inventory.countItem(Items.DIAMOND) == 0, "diamante ficou no inventário")
            h.assertTrue(SnapshotStorage.has(p.uuid), "sem arquivo de inventário salvo")
            h.assertTrue(m.players.getValue(p.uuid).money == 500, "dinheiro=${m.players.getValue(p.uuid).money}")
        }
        val sCt = arena.spawnsCt[0]
        val sTr = arena.spawnsTr[0]
        h.assertTrue(ct.position().distanceTo(Vec3(sCt.x, sCt.y, sCt.z)) < 1.0, "CT fora do spawn: ${ct.position()}")
        h.assertTrue(tr.position().distanceTo(Vec3(sTr.x, sTr.y, sTr.z)) < 1.0, "TR fora do spawn: ${tr.position()}")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "partida_cancelar")
    fun cancelar_devolve_tudo(h: GameTestHelper) {
        val (m, ct, tr) = Fixtures.started(h)
        val saida = m.arena.saida!!
        h.assertTrue(MatchManager.cancel(h.level.server) == null, "cancel deu erro")
        for (p in listOf(ct, tr)) {
            h.assertTrue(modo(p) == GameType.SURVIVAL, "modo=${modo(p)}")
            h.assertTrue(p.inventory.countItem(Items.DIAMOND) == 1, "diamante não voltou")
            h.assertTrue(p.position().distanceTo(Vec3(saida.x, saida.y, saida.z)) < 1.0, "não foi para a saída: ${p.position()}")
            h.assertTrue(!SnapshotStorage.has(p.uuid), "arquivo ficou")
        }
        h.assertTrue(MatchManager.match == null, "partida não foi apagada")
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "partida_minimo")
    fun minimo_de_jogadores(h: GameTestHelper) {
        Fixtures.match(h, Fixtures.config { minimoJogadores = 2 })
        MatchManager.join(Fixtures.player(h))
        val err = MatchManager.start(h.level.server)
        h.assertTrue(err != null && "mínimo" in err, "erro=$err")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "partida_entrar_tarde")
    fun nao_entra_depois_de_iniciar(h: GameTestHelper) {
        Fixtures.started(h)
        h.assertTrue(MatchManager.join(Fixtures.player(h)) != null, "entrou depois de iniciar")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "partida_sair")
    fun sair_no_meio_devolve(h: GameTestHelper) {
        val (m, ct, _) = Fixtures.started(h)
        h.assertTrue(MatchManager.leave(ct) == null, "leave deu erro")
        h.assertTrue(ct.uuid !in m.players, "continua na partida")
        h.assertTrue(ct.inventory.countItem(Items.DIAMOND) == 1, "diamante não voltou")
        h.assertTrue(modo(ct) == GameType.SURVIVAL, "modo=${modo(ct)}")
        Fixtures.reset(h)
        h.succeed()
    }
}

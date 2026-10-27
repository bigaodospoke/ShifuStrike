package com.shifustrike.gametest

import com.shifustrike.core.Team
import com.shifustrike.data.SnapshotStorage
import com.shifustrike.gametest.Fixtures.modo
import com.shifustrike.match.Match
import com.shifustrike.match.MatchManager
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import com.shifustrike.pointblank.EventItems
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
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

    /** Se o servidor cair logo depois da devolução, o disco já tem o inventário devolvido (não o da partida). */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "partida_salva_disco")
    fun restauracao_salva_no_disco(h: GameTestHelper) {
        val (_, ct, _) = Fixtures.started(h)
        MatchManager.cancel(h.level.server)
        val f = h.level.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve("${ct.uuid}.dat")
        h.assertTrue(Files.exists(f), "os dados do jogador não foram gravados depois da devolução")
        val inv = NbtIo.readCompressed(f, NbtAccounter.unlimitedHeap()).getList("Inventory", Tag.TAG_COMPOUND.toInt())
        h.assertTrue((0 until inv.size).any { inv.getCompound(it).getString("id") == "minecraft:diamond" }, "o disco não tem o diamante devolvido")
        h.succeed()
    }

    /** Item na grade de criação do inventário: o do jogador não some no início, o do evento não vaza no fim. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "partida_grade")
    fun grade_de_criacao_nao_perde_nem_vaza(h: GameTestHelper) {
        Fixtures.match(h)
        val a = Fixtures.player(h)
        val b = Fixtures.player(h)
        a.inventoryMenu.craftSlots.setItem(0, ItemStack(Items.EMERALD))
        MatchManager.join(a)
        MatchManager.join(b)
        MatchManager.start(h.level.server)
        val awp = BuiltInRegistries.ITEM.get(ResourceLocation.parse("pointblank:l96a1"))
        a.inventoryMenu.craftSlots.setItem(1, EventItems.mark(ItemStack(awp)))
        MatchManager.cancel(h.level.server)
        val grade = a.inventoryMenu.craftSlots
        val esmeraldas = a.inventory.countItem(Items.EMERALD) + (0 until grade.containerSize).sumOf { if (grade.getItem(it).`is`(Items.EMERALD)) grade.getItem(it).count else 0 }
        h.assertTrue(esmeraldas == 1, "esmeralda do jogador: $esmeraldas")
        h.assertTrue(a.inventory.countItem(awp) == 0 && (0 until grade.containerSize).none { grade.getItem(it).`is`(awp) }, "a AWP do evento vazou")
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

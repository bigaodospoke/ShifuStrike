package com.shifustrike.gametest

import com.shifustrike.gametest.Fixtures.modo
import com.shifustrike.match.Loadouts
import com.shifustrike.match.MatchManager
import com.shifustrike.pointblank.EventItems
import com.shifustrike.pointblank.PointBlankItems
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

class GuardGameTest : FabricGameTest {
    private fun item(id: String) = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))

    /** Cubo 3x3x3 de pedra em volta de `centro` (relativo); devolve as posições absolutas. */
    private fun cubo(h: GameTestHelper, centro: BlockPos): List<BlockPos> {
        val out = mutableListOf<BlockPos>()
        for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
            if (dx == 0 && dy == 0 && dz == 0) continue
            val rel = centro.offset(dx, dy, dz)
            h.setBlock(rel, Blocks.STONE)
            out += h.absolutePos(rel)
        }
        return out
    }

    private fun explode(h: GameTestHelper, centro: BlockPos) {
        val c = h.absoluteVec(Vec3.atCenterOf(centro))
        h.level.explode(null, c.x, c.y, c.z, 3f, Level.ExplosionInteraction.TNT)
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "guarda_explosao")
    fun explosao_na_area_nao_quebra(h: GameTestHelper) {
        Fixtures.started(h)
        val blocos = cubo(h, BlockPos(4, 3, 4))
        explode(h, BlockPos(4, 3, 4))
        h.assertTrue(blocos.all { h.level.getBlockState(it).`is`(Blocks.STONE) }, "a explosão quebrou bloco do mapa")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "guarda_explosao_fora")
    fun explosao_fora_da_area_quebra_normal(h: GameTestHelper) {
        Fixtures.started(h)
        // a área do teste vai até z=12; z=18 está fora
        val fora = cubo(h, BlockPos(4, 3, 18))
        explode(h, BlockPos(4, 3, 18))
        h.assertTrue(fora.any { h.level.getBlockState(it).isAir }, "explosão fora da área não quebrou nada")
        Fixtures.reset(h)
        val dentro = cubo(h, BlockPos(4, 3, 4))
        explode(h, BlockPos(4, 3, 4))
        h.assertTrue(dentro.any { h.level.getBlockState(it).isAir }, "sem partida a área continuou protegida")
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "guarda_vidro")
    fun bala_nao_quebra_vidro(h: GameTestHelper) {
        val (_, ct, _) = Fixtures.started(h)
        h.setBlock(BlockPos(4, 2, 4), Blocks.GLASS)
        val abs = h.absolutePos(BlockPos(4, 2, 4))
        h.assertTrue(!h.level.destroyBlock(abs, true, ct), "destroyBlock devolveu true")
        h.assertTrue(h.level.getBlockState(abs).`is`(Blocks.GLASS), "o vidro quebrou")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "guarda_de_fora", timeoutTicks = 200)
    fun de_fora_e_tirado_da_area(h: GameTestHelper) {
        val (m, _, _) = Fixtures.started(h)
        val curioso = Fixtures.player(h)
        val staff = Fixtures.player(h)
        val dentro = h.absoluteVec(Vec3(4.5, 2.0, 4.5))
        curioso.teleportTo(h.level, dentro.x, dentro.y, dentro.z, 0f, 0f)
        staff.teleportTo(h.level, dentro.x, dentro.y, dentro.z, 0f, 0f)
        staff.setGameMode(GameType.SPECTATOR)
        h.runAfterDelay(25) {
            val s = m.arena.saida!!
            h.assertTrue(curioso.position().distanceTo(Vec3(s.x, s.y, s.z)) < 1.0, "curioso continua na área: ${curioso.position()}")
            h.assertTrue(staff.position().distanceTo(dentro) < 0.1, "staff espectador foi tirada")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "guarda_vivo_sai", timeoutTicks = 200)
    fun vivo_que_sai_volta_ao_spawn(h: GameTestHelper) {
        val (m, ct, _) = Fixtures.started(h)
        val longe = h.absoluteVec(Vec3(4.5, 2.0, 40.5))
        ct.teleportTo(h.level, longe.x, longe.y, longe.z, 0f, 0f)
        h.runAfterDelay(25) {
            val s = m.arena.spawnsCt[0]
            h.assertTrue(ct.position().distanceTo(Vec3(s.x, s.y, s.z)) < 1.0, "CT não voltou ao spawn: ${ct.position()}")
            Fixtures.reset(h)
            h.succeed()
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "guarda_segunda_primaria")
    fun nao_pega_segunda_primaria(h: GameTestHelper) {
        val (m, _, tr) = Fixtures.started(h)
        Loadouts.giveWeapon(tr, m.shop.first { it.chave == "ak47" }, null)
        val galil = EventItems.mark(PointBlankItems.loadedGun(tr, "pointblank:ak74", 30)!!)
        val e = ItemEntity(h.level, tr.x, tr.y, tr.z, galil)
        e.setNoPickUpDelay()
        h.level.addFreshEntity(e)
        e.playerTouch(tr)
        h.assertTrue(tr.inventory.countItem(item("pointblank:ak74")) == 0, "pegou a segunda primária")
        h.assertTrue(!e.isRemoved, "a galil sumiu do chão")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "guarda_bau")
    fun nao_abre_bau(h: GameTestHelper) {
        val (_, ct, _) = Fixtures.started(h)
        h.setBlock(BlockPos(4, 2, 4), Blocks.CHEST)
        val abs = h.absolutePos(BlockPos(4, 2, 4))
        val hit = BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)
        val r = UseBlockCallback.EVENT.invoker().interact(ct, h.level, InteractionHand.MAIN_HAND, hit)
        h.assertTrue(r == InteractionResult.FAIL, "participante abriu baú: $r")
        h.assertTrue(modo(ct) == GameType.ADVENTURE, "modo=${modo(ct)}")
        Fixtures.reset(h)
        h.succeed()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "guarda_comando")
    fun comando_bloqueado_na_partida(h: GameTestHelper) {
        val (_, ct, _) = Fixtures.started(h)
        val antes = ct.gameMode.gameModeForPlayer
        h.level.server.commands.performPrefixedCommand(ct.createCommandSourceStack().withPermission(4), "gamemode creative")
        h.assertTrue(ct.gameMode.gameModeForPlayer == antes, "comando de fora da lista rodou na partida")
        h.assertTrue(MatchManager.isPlaying(ct), "saiu da partida")
        Fixtures.reset(h)
        h.succeed()
    }
}

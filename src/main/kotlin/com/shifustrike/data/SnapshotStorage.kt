package com.shifustrike.data

import com.shifustrike.ShifuStrike
import com.shifustrike.arena.Ponto
import com.shifustrike.mixin.PlayerListInvoker
import com.shifustrike.pointblank.EventItems
import com.shifustrike.util.SafeFiles
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** <mundo>/shifustrike/salvos/<uuid>.dat — gravado na hora em que o jogador entra na partida. */
object SnapshotStorage {
    private var dir: Path? = null

    fun init(server: MinecraftServer) {
        val d = server.getWorldPath(LevelResource.ROOT).resolve(ShifuStrike.MOD_ID).resolve("salvos")
        Files.createDirectories(d)
        dir = d
    }

    private fun file(id: UUID): Path = (dir ?: error("SnapshotStorage sem init")).resolve("$id.dat")

    fun save(id: UUID, s: PlayerSnapshot) = SafeFiles.writeAtomic(file(id)) { NbtIo.writeCompressed(s.tag, it) }

    fun load(id: UUID): PlayerSnapshot? {
        val f = file(id)
        if (!Files.exists(f)) return null
        return try {
            PlayerSnapshot(NbtIo.readCompressed(f, NbtAccounter.unlimitedHeap()))
        } catch (e: Exception) {
            ShifuStrike.LOGGER.error("Não deu para ler o inventário salvo de {} ({}) — o arquivo fica para recuperar à mão", id, e.message)
            null
        }
    }

    fun delete(id: UUID) {
        Files.deleteIfExists(file(id))
    }

    fun has(id: UUID): Boolean = Files.exists(file(id))

    /**
     * Devolve o que foi guardado, grava o jogador no disco e só então apaga o salvo: se o servidor cair logo depois,
     * o disco já tem o inventário devolvido. Devolve false se não havia salvo legível.
     */
    fun restoreAndForget(p: ServerPlayer, exit: Ponto?, exitLevel: ServerLevel?): Boolean {
        val s = load(p.uuid) ?: return false
        s.restore(p, exit, exitLevel)
        (p.server.playerList as PlayerListInvoker).`shifustrike$save`(p)
        delete(p.uuid)
        return true
    }

    /** Jogador entrando no servidor fora de uma partida: devolve o que foi guardado ou, sem salvo, tira itens do evento. */
    fun restoreOnJoin(p: ServerPlayer, exit: Ponto?, exitLevel: ServerLevel?) {
        if (restoreAndForget(p, exit, exitLevel)) {
            ShifuStrike.LOGGER.info("Inventário de {} devolvido depois do evento", p.gameProfile.name)
        } else {
            EventItems.purge(p)
        }
    }
}

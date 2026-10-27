package com.shifustrike.arena

import com.shifustrike.ShifuStrike
import com.shifustrike.config.ConfigManager
import com.shifustrike.util.SafeFiles
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path

/** <mundo>/shifustrike/mapa.json */
object ArenaStorage {
    var arena = ArenaData()
        private set
    private var file: Path? = null

    fun load(server: MinecraftServer) {
        val f = server.getWorldPath(LevelResource.ROOT).resolve(ShifuStrike.MOD_ID).resolve("mapa.json")
        file = f
        arena = if (Files.exists(f)) {
            try {
                Files.newBufferedReader(f).use { ConfigManager.GSON.fromJson(it, ArenaData::class.java) } ?: ArenaData()
            } catch (e: Exception) {
                ShifuStrike.LOGGER.error("Erro lendo {} — mapa vazio. ({})", f, e.message)
                ArenaData()
            }
        } else {
            ArenaData()
        }
    }

    fun save() {
        val f = file ?: return
        SafeFiles.writeAtomic(f, ConfigManager.GSON.toJson(arena))
    }
}

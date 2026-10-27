package com.shifustrike.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.shifustrike.ShifuStrike
import com.shifustrike.core.ShopEntry
import com.shifustrike.util.SafeFiles
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import java.nio.file.Files
import java.nio.file.Path

object ConfigManager {
    val GSON: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    var config = StrikeConfig()
        private set
    var shop: List<ShopEntry> = emptyList()
        private set

    /** Entradas da loja tiradas porque o item ou a munição não existem (PointBlank ausente ou id errado). */
    var removedFromShop: List<String> = emptyList()
        private set

    private fun dir(): Path = FabricLoader.getInstance().configDir.resolve(ShifuStrike.MOD_ID)

    fun load() {
        val file = dir().resolve("config.json")
        config = if (Files.exists(file)) {
            try {
                Files.newBufferedReader(file).use { GSON.fromJson(it, StrikeConfig::class.java) } ?: StrikeConfig()
            } catch (e: Exception) {
                ShifuStrike.LOGGER.error("Erro lendo config/shifustrike/config.json — usando padrão. Corrija o JSON! ({})", e.message)
                StrikeConfig()
            }
        } else {
            StrikeConfig()
        }
        config.sanitize()
        try {
            // Regrava para incluir opções novas com o valor padrão
            SafeFiles.writeAtomic(file, GSON.toJson(config))
        } catch (e: Exception) {
            ShifuStrike.LOGGER.error("Não foi possível salvar config/shifustrike/config.json", e)
        }
        shop = ShopConfig.load(dir())
    }

    private fun exists(id: String?): Boolean =
        id == null || (ResourceLocation.tryParse(id)?.let { BuiltInRegistries.ITEM.containsKey(it) } ?: false)

    /** A loja sem as entradas cujos itens não existem neste servidor. Precisa dos registros já carregados. */
    fun activeShop(): List<ShopEntry> {
        val (ok, missing) = shop.partition { exists(it.item) && exists(it.municao) }
        removedFromShop = missing.map { "${it.chave} (${it.item ?: "-"} / ${it.municao ?: "-"})" }
        if (missing.isNotEmpty()) ShifuStrike.LOGGER.warn("Tirados da loja porque o item não existe: {}", removedFromShop.joinToString())
        return ok
    }
}

package com.shifustrike.config

import com.google.gson.reflect.TypeToken
import com.shifustrike.ShifuStrike
import com.shifustrike.core.DefaultShop
import com.shifustrike.core.ShopEntry
import com.shifustrike.core.ShopValidation
import com.shifustrike.util.SafeFiles
import java.nio.file.Files
import java.nio.file.Path

/** config/shifustrike/loja.json — armas, preços e posições da tela de compra. */
object ShopConfig {
    private val TYPE = object : TypeToken<List<ShopEntry>>() {}.type

    fun load(dir: Path): List<ShopEntry> {
        val file = dir.resolve("loja.json")
        if (!Files.exists(file)) {
            save(dir, DefaultShop.entries)
            return DefaultShop.entries
        }
        val entries: List<ShopEntry> = try {
            Files.newBufferedReader(file).use { ConfigManager.GSON.fromJson<List<ShopEntry>>(it, TYPE) } ?: emptyList()
        } catch (e: Exception) {
            ShifuStrike.LOGGER.error("Erro lendo config/shifustrike/loja.json — usando a loja padrão. Corrija o JSON! ({})", e.message)
            return DefaultShop.entries
        }
        val problems = try {
            ShopValidation.problems(entries)
        } catch (e: NullPointerException) {
            listOf("campo obrigatório faltando (${e.message})")
        }
        if (problems.isNotEmpty()) {
            ShifuStrike.LOGGER.error("loja.json tem problemas, usando a loja padrão:\n  {}", problems.joinToString("\n  "))
            return DefaultShop.entries
        }
        return entries
    }

    fun save(dir: Path, entries: List<ShopEntry>) = SafeFiles.writeAtomic(dir.resolve("loja.json"), ConfigManager.GSON.toJson(entries, TYPE))
}

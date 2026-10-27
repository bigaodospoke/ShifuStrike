package com.shifustrike.config

import com.shifustrike.arena.ArenaData
import com.shifustrike.core.DefaultShop
import com.shifustrike.core.RoundTimings
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigTest {
    @Test
    fun `config padrao vira tempos em ticks`() = assertEquals(RoundTimings(200, 600, 2300, 800, 100), StrikeConfig().timings())

    @Test
    fun `config padrao vira economia do CS`() {
        val e = StrikeConfig().economy()
        assertEquals(500, e.startMoney)
        assertEquals(3400, e.lossMax)
    }

    @Test
    fun `sanitize corrige valores ruins`() {
        val c = StrikeConfig().apply {
            vitoriasParaVencer = 0
            segundosCongelado = 10
            segundosCompra = 4
        }
        c.sanitize()
        assertEquals(1, c.vitoriasParaVencer)
        assertEquals(10, c.segundosCompra)
    }

    @Test
    fun `loja vai e volta do json igual`(@TempDir dir: Path) {
        ShopConfig.save(dir, DefaultShop.entries)
        assertEquals(DefaultShop.entries, ShopConfig.load(dir))
    }

    @Test
    fun `loja sem arquivo grava o padrao`(@TempDir dir: Path) {
        assertEquals(DefaultShop.entries, ShopConfig.load(dir))
        assertTrue(Files.exists(dir.resolve("loja.json")))
    }

    @Test
    fun `loja invalida cai no padrao`(@TempDir dir: Path) {
        val ruim = DefaultShop.entries + DefaultShop.entries.first { it.chave == "deagle" }.copy(chave = "outra")
        ShopConfig.save(dir, ruim)
        assertEquals(DefaultShop.entries, ShopConfig.load(dir))
    }

    @Test
    fun `mapa incompleto lista o que falta`() {
        val p = ArenaData().problemas().joinToString("\n")
        for (f in listOf("spawn CT", "spawn TR", "bomba A", "bomba B", "área", "saída")) assertTrue(f in p, "faltou '$f' em:\n$p")
    }
}

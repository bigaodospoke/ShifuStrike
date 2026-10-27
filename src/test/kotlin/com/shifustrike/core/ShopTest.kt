package com.shifustrike.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopTest {
    @Test
    fun `cada time tem 23 entradas em posicoes unicas`() {
        for (t in Team.entries) {
            val loja = DefaultShop.entries.forTeam(t)
            assertEquals(23, loja.size, "time $t")
            assertEquals(23, loja.map { it.coluna to it.posicao }.toSet().size, "posições repetidas no $t")
        }
    }

    @Test
    fun `ak47 do TR`() {
        val ak = DefaultShop.entries.forTeam(Team.TR).single { it.chave == "ak47" }
        assertEquals("pointblank:ak47", ak.item)
        assertEquals("pointblank:ammo762", ak.municao)
        assertEquals(30, ak.pente)
        assertEquals(90, ak.reserva)
        assertEquals(2700, ak.preco)
        assertEquals(4 to 2, ak.coluna to ak.posicao)
        assertEquals(EntryKind.PRIMARIA, ak.tipo)
    }

    @Test
    fun `pistola inicial de cada time`() {
        assertEquals("glock18", DefaultShop.starterKey(Team.TR))
        assertEquals("usp_s", DefaultShop.starterKey(Team.CT))
        assertEquals("pointblank:usp45", DefaultShop.entries.forTeam(Team.CT).single { it.chave == "usp_s" }.item)
    }

    @Test
    fun `categorias de kill`() {
        val ct = DefaultShop.entries.forTeam(Team.CT).associateBy { it.chave }
        val tr = DefaultShop.entries.forTeam(Team.TR).associateBy { it.chave }
        assertEquals(KillCategory.SMG, tr.getValue("mac10").categoria)
        assertEquals(KillCategory.SMG, ct.getValue("p90").categoria)
        assertEquals(KillCategory.ESCOPETA, tr.getValue("nova").categoria)
        assertEquals(KillCategory.AWP, ct.getValue("awp").categoria)
        assertEquals(KillCategory.PADRAO, ct.getValue("m4a4").categoria)
    }

    @Test
    fun `catalogo padrao sem problemas`() = assertEquals(emptyList(), ShopValidation.problems(DefaultShop.entries))

    @Test
    fun `validacao acha posicao repetida`() {
        val a = DefaultShop.entries.first { it.chave == "berettas" }
        val dup = a.copy(chave = "outra", time = Team.CT)
        val problemas = ShopValidation.problems(DefaultShop.entries + dup)
        assertEquals(1, problemas.size, problemas.toString())
    }

    @Test
    fun `validacao exige item nas armas`() {
        val semItem = DefaultShop.entries.map { if (it.chave == "deagle") it.copy(item = null) else it }
        assertTrue(ShopValidation.problems(semItem).any { "deagle" in it })
    }
}

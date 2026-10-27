package com.shifustrike.core

import kotlin.test.Test
import kotlin.test.assertEquals

class BuyRulesTest {
    private fun entrada(time: Team, chave: String) = DefaultShop.entries.forTeam(time).single { it.chave == chave }
    private val nada = Owned(null, null, 0, false, 0)
    private fun ctx(
        time: Team = Team.TR, dinheiro: Int = 16000, vivo: Boolean = true,
        compra: Boolean = true, zona: Boolean = true, tem: Owned = nada,
    ) = BuyContext(time, dinheiro, vivo, compra, zona, tem)

    @Test
    fun `compra normal`() = assertEquals(BuyCheck.Ok(2700, null), BuyRules.check(entrada(Team.TR, "ak47"), ctx()))

    @Test
    fun `capacete sozinho custa 350`() {
        val cc = entrada(Team.CT, "colete_capacete")
        assertEquals(350, BuyRules.price(cc, Owned(null, null, 100, false, 0)))
        assertEquals(1000, BuyRules.price(cc, Owned(null, null, 60, false, 0)))
        assertEquals(1000, BuyRules.price(cc, nada))
    }

    @Test
    fun `ordem das falhas`() {
        val ak = entrada(Team.TR, "ak47")
        assertEquals(BuyCheck.Fail(BuyFail.MORTO), BuyRules.check(ak, ctx(vivo = false, dinheiro = 0)))
        assertEquals(BuyCheck.Fail(BuyFail.FORA_DO_TEMPO), BuyRules.check(ak, ctx(compra = false, zona = false)))
        assertEquals(BuyCheck.Fail(BuyFail.FORA_DA_ZONA), BuyRules.check(ak, ctx(zona = false, dinheiro = 0)))
        assertEquals(BuyCheck.Fail(BuyFail.SEM_DINHEIRO), BuyRules.check(ak, ctx(dinheiro = 2699)))
    }

    @Test
    fun `mesma arma duas vezes`() =
        assertEquals(BuyCheck.Fail(BuyFail.JA_TEM), BuyRules.check(entrada(Team.TR, "ak47"), ctx(tem = nada.copy(primaria = "ak47"))))

    @Test
    fun `trocar primaria`() =
        assertEquals(BuyCheck.Ok(2700, "galil"), BuyRules.check(entrada(Team.TR, "ak47"), ctx(tem = nada.copy(primaria = "galil"))))

    @Test
    fun `trocar pistola`() =
        assertEquals(BuyCheck.Ok(700, "glock18"), BuyRules.check(entrada(Team.TR, "deagle"), ctx(tem = nada.copy(pistola = "glock18"))))

    @Test
    fun `colete cheio nao compra de novo`() {
        assertEquals(BuyCheck.Fail(BuyFail.JA_TEM), BuyRules.check(entrada(Team.CT, "colete"), ctx(Team.CT, tem = nada.copy(armor = 100))))
        assertEquals(
            BuyCheck.Fail(BuyFail.JA_TEM),
            BuyRules.check(entrada(Team.CT, "colete_capacete"), ctx(Team.CT, tem = nada.copy(armor = 100, helmet = true))),
        )
        assertEquals(BuyCheck.Ok(350, null), BuyRules.check(entrada(Team.CT, "colete_capacete"), ctx(Team.CT, tem = nada.copy(armor = 100))))
    }

    @Test
    fun `uma granada so`() =
        assertEquals(BuyCheck.Fail(BuyFail.LIMITE), BuyRules.check(entrada(Team.TR, "he"), ctx(tem = nada.copy(granadas = 1))))

    @Test
    fun `em breve nao compra`() =
        assertEquals(BuyCheck.Fail(BuyFail.EM_BREVE), BuyRules.check(entrada(Team.CT, "zeus"), ctx(Team.CT)))

    @Test
    fun `time errado`() =
        assertEquals(BuyCheck.Fail(BuyFail.TIME_ERRADO), BuyRules.check(entrada(Team.TR, "ak47"), ctx(Team.CT)))
}

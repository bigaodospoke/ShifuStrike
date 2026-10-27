package com.shifustrike.config

import com.shifustrike.core.EconomyRules
import com.shifustrike.core.KillCategory
import com.shifustrike.core.RoundTimings

/** config/shifustrike/config.json — tempos em segundos, dinheiro em $, raios em blocos. */
class StrikeConfig {
    var segundosCongelado = 10
    var segundosCompra = 30
    var segundosRodada = 115
    var segundosBomba = 40
    var segundosPlantar = 3
    var segundosDesarmar = 5
    var segundosFimRodada = 5

    var vitoriasParaVencer = 5

    /** Jogadores necessários para o /cs iniciar. Use 1 só para testar sozinho. */
    var minimoJogadores = 2

    var dinheiroInicial = 500
    var dinheiroMaximo = 16000
    var vitoriaEliminacao = 3250
    var vitoriaBomba = 3500
    var derrotaBase = 1400
    var derrotaPasso = 500
    var derrotaMaxima = 3400
    var bonusPlantio = 800

    var killPadrao = 300
    var killSmg = 600
    var killEscopeta = 900
    var killAwp = 100
    var killFaca = 1500

    /** Distância máxima de um spawn do próprio time para poder comprar. */
    var raioZonaCompra = 15.0

    /** Distância máxima da bomba para desarmar. */
    var raioDesarme = 2.0

    var raioExplosaoC4 = 12.0

    /** Dano da C4 no centro, em pontos do CS (vida 100); cai até 0 na borda do raio. */
    var danoMaximoC4 = 500f

    /** Parte do dano que passa para a vida quando o jogador tem colete. */
    var proporcaoColete = 0.5f

    /** Pontos de colete gastos por ponto de dano segurado. */
    var bonusColete = 0.5f

    /** Comandos que os jogadores da partida podem usar (o resto fica bloqueado). */
    var comandosPermitidos = listOf("cs", "msg", "tell", "w", "r")

    fun sanitize() {
        segundosCongelado = segundosCongelado.coerceIn(0, 60)
        segundosCompra = segundosCompra.coerceAtLeast(segundosCongelado)
        segundosRodada = segundosRodada.coerceAtLeast(1)
        segundosBomba = segundosBomba.coerceAtLeast(1)
        segundosPlantar = segundosPlantar.coerceIn(1, 20)
        segundosDesarmar = segundosDesarmar.coerceIn(1, 20)
        segundosFimRodada = segundosFimRodada.coerceIn(1, 30)
        vitoriasParaVencer = vitoriasParaVencer.coerceIn(1, 30)
        minimoJogadores = minimoJogadores.coerceAtLeast(1)
        dinheiroInicial = dinheiroInicial.coerceAtLeast(0)
        dinheiroMaximo = dinheiroMaximo.coerceAtLeast(dinheiroInicial)
        raioZonaCompra = raioZonaCompra.coerceAtLeast(1.0)
        raioDesarme = raioDesarme.coerceIn(1.0, 6.0)
        raioExplosaoC4 = raioExplosaoC4.coerceAtLeast(1.0)
        danoMaximoC4 = danoMaximoC4.coerceAtLeast(0f)
        proporcaoColete = proporcaoColete.coerceIn(0f, 1f)
        bonusColete = bonusColete.coerceIn(0.05f, 1f)
        comandosPermitidos = comandosPermitidos.map { it.trim().removePrefix("/").lowercase() }.filter { it.isNotEmpty() }
        if ("cs" !in comandosPermitidos) comandosPermitidos = comandosPermitidos + "cs"
    }

    fun timings() = RoundTimings(
        freezeTicks = segundosCongelado * 20,
        buyTicks = segundosCompra * 20,
        roundTicks = segundosRodada * 20,
        bombTicks = segundosBomba * 20,
        roundEndTicks = segundosFimRodada * 20,
    )

    fun economy() = EconomyRules(
        startMoney = dinheiroInicial,
        maxMoney = dinheiroMaximo,
        winElimination = vitoriaEliminacao,
        winBomb = vitoriaBomba,
        lossBase = derrotaBase,
        lossStep = derrotaPasso,
        lossMax = derrotaMaxima,
        plantBonus = bonusPlantio,
        killRewards = mapOf(
            KillCategory.PADRAO to killPadrao,
            KillCategory.SMG to killSmg,
            KillCategory.ESCOPETA to killEscopeta,
            KillCategory.AWP to killAwp,
            KillCategory.FACA to killFaca,
        ),
    )
}

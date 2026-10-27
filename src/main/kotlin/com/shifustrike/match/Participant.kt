package com.shifustrike.match

import com.shifustrike.core.Team
import java.util.UUID

/** Compra feita nesta rodada (para o reembolso). `stackId` marca a arma entregue. */
data class Purchase(val chave: String, val price: Int, val stackId: UUID?)

/** Um jogador dentro da partida já iniciada. */
class Participant(val id: UUID, var name: String, var team: Team) {
    var money = 0
    var alive = false
    var connected = true

    /** Colete em pontos do CS (0-100) e capacete. Não são itens: ficam só aqui e no HUD. */
    var armor = 0
    var helmet = false

    var kills = 0
    var deaths = 0

    /** Se o último dano recebido foi tiro na cabeça (para o feed de kills). */
    var lastHitHeadshot = false

    val purchases = mutableListOf<Purchase>()
}

package com.shifustrike.core

/** O que o jogador tem agora: chaves da loja da primária e da pistola, colete (0-100), capacete e granadas. */
data class Owned(val primaria: String?, val pistola: String?, val armor: Int, val helmet: Boolean, val granadas: Int)

data class BuyContext(
    val team: Team,
    val money: Int,
    val alive: Boolean,
    val buyOpen: Boolean,
    val inZone: Boolean,
    val owned: Owned,
)

enum class BuyFail { MORTO, FORA_DO_TEMPO, FORA_DA_ZONA, TIME_ERRADO, EM_BREVE, JA_TEM, LIMITE, SEM_DINHEIRO }

sealed interface BuyCheck {
    /** `replaces` = chave da arma que sai do slot (vai para o chão). */
    data class Ok(val price: Int, val replaces: String?) : BuyCheck
    data class Fail(val reason: BuyFail) : BuyCheck
}

object BuyRules {
    fun price(e: ShopEntry, o: Owned): Int =
        if (e.tipo == EntryKind.COLETE_CAPACETE && o.armor >= 100 && !o.helmet) e.precoAlt ?: e.preco else e.preco

    fun check(e: ShopEntry, ctx: BuyContext, maxGrenades: Int = 1): BuyCheck {
        val o = ctx.owned
        val fail = when {
            !ctx.alive -> BuyFail.MORTO
            !ctx.buyOpen -> BuyFail.FORA_DO_TEMPO
            !ctx.inZone -> BuyFail.FORA_DA_ZONA
            e.time != null && e.time != ctx.team -> BuyFail.TIME_ERRADO
            e.tipo == EntryKind.EM_BREVE -> BuyFail.EM_BREVE
            e.chave == o.primaria || e.chave == o.pistola -> BuyFail.JA_TEM
            e.tipo == EntryKind.COLETE && o.armor >= 100 -> BuyFail.JA_TEM
            e.tipo == EntryKind.COLETE_CAPACETE && o.armor >= 100 && o.helmet -> BuyFail.JA_TEM
            e.tipo == EntryKind.GRANADA && o.granadas >= maxGrenades -> BuyFail.LIMITE
            ctx.money < price(e, o) -> BuyFail.SEM_DINHEIRO
            else -> null
        }
        if (fail != null) return BuyCheck.Fail(fail)
        val replaces = when (e.tipo) {
            EntryKind.PRIMARIA -> o.primaria
            EntryKind.PISTOLA -> o.pistola
            else -> null
        }
        return BuyCheck.Ok(price(e, o), replaces)
    }
}

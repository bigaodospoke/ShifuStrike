package com.shifustrike.core

enum class EntryKind { PRIMARIA, PISTOLA, GRANADA, COLETE, COLETE_CAPACETE, EM_BREVE }

/**
 * Um quadrado da tela de compra. `time = null` aparece para os dois times.
 * `pente` = balas carregadas na arma; `reserva` = balas extras entregues em itens de munição.
 */
data class ShopEntry(
    val chave: String,
    val time: Team?,
    val coluna: Int,
    val posicao: Int,
    val nome: String,
    val preco: Int,
    val tipo: EntryKind,
    val item: String? = null,
    val municao: String? = null,
    val pente: Int = 0,
    val reserva: Int = 0,
    val categoria: KillCategory = KillCategory.PADRAO,
    val icone: String,
    /** Só para COLETE_CAPACETE: preço quando o jogador já tem colete cheio. */
    val precoAlt: Int? = null,
)

fun List<ShopEntry>.forTeam(team: Team): List<ShopEntry> = filter { it.time == null || it.time == team }

/** Catálogo aprovado (spec, seção 9). Pente e munição medidos no PointBlank 2.2.0; reserva como no CS. */
object DefaultShop {
    private fun arma(
        chave: String, time: Team?, coluna: Int, posicao: Int, nome: String, preco: Int, pb: String,
        municao: String, pente: Int, reserva: Int, categoria: KillCategory = KillCategory.PADRAO,
    ) = ShopEntry(
        chave, time, coluna, posicao, nome, preco,
        if (coluna == 2) EntryKind.PISTOLA else EntryKind.PRIMARIA,
        item = "pointblank:$pb", municao = "pointblank:$municao", pente = pente, reserva = reserva,
        categoria = categoria, icone = "pointblank:textures/item/$pb.icon.png",
    )

    private fun emBreve(chave: String, time: Team?, coluna: Int, posicao: Int, nome: String, preco: Int) =
        ShopEntry(chave, time, coluna, posicao, nome, preco, EntryKind.EM_BREVE, icone = "shifustrike:textures/gui/icone/$chave.png")

    private val CT = Team.CT
    private val TR = Team.TR
    private val SMG = KillCategory.SMG
    private val ESC = KillCategory.ESCOPETA

    val entries: List<ShopEntry> = listOf(
        ShopEntry("colete", null, 1, 1, "Colete de Kevlar", 650, EntryKind.COLETE, icone = "shifustrike:textures/gui/icone/colete.png"),
        ShopEntry(
            "colete_capacete", null, 1, 2, "Colete + Capacete", 1000, EntryKind.COLETE_CAPACETE,
            icone = "shifustrike:textures/gui/icone/colete_capacete.png", precoAlt = 350,
        ),
        emBreve("zeus", null, 1, 3, "Zeus x27", 200),

        arma("usp_s", CT, 2, 1, "USP-S", 200, "usp45", "ammo45acp", 12, 24),
        arma("glock18", TR, 2, 1, "Glock-18", 200, "glock18", "ammo9mm", 20, 120),
        arma("berettas", null, 2, 2, "Berettas Duplas", 300, "m9", "ammo9mm", 15, 120),
        arma("p250", null, 2, 3, "P250", 300, "p30l", "ammo9mm", 15, 26),
        arma("five_seven", CT, 2, 4, "Five-SeveN", 500, "tti_viper", "ammo45acp", 21, 100),
        arma("tec9", TR, 2, 4, "Tec-9", 500, "m1911a1", "ammo45acp", 7, 90),
        arma("deagle", null, 2, 5, "Desert Eagle", 700, "deserteagle", "ammo50ae", 8, 35),

        arma("mag7", CT, 3, 1, "MAG-7", 1300, "m590", "ammo12gauge", 7, 32, ESC),
        arma("nova", TR, 3, 1, "Nova", 1050, "m870", "ammo12gauge", 6, 32, ESC),
        arma("xm1014", null, 3, 2, "XM1014", 2000, "m1014", "ammo12gauge", 8, 32, ESC),
        arma("mp7", CT, 3, 3, "MP7", 1400, "mp7", "ammo46", 40, 120, SMG),
        arma("mp5sd", TR, 3, 3, "MP5-SD", 1500, "mp5", "ammo9mm", 30, 120, SMG),
        arma("p90", null, 3, 4, "P90", 2350, "p90", "ammo57", 50, 100, SMG),
        arma("mp9", CT, 3, 5, "MP9", 1250, "tmp", "ammo9mm", 30, 120, SMG),
        arma("mac10", TR, 3, 5, "MAC-10", 1050, "vector", "ammo45acp", 25, 100, SMG),

        arma("ssg08", CT, 4, 1, "SSG 08", 1700, "c14", "ammo338lapua", 5, 90),
        arma("galil", TR, 4, 1, "Galil AR", 1800, "ak74", "ammo545", 30, 90),
        arma("m4a1s", CT, 4, 2, "M4A1-S", 2900, "hk416", "ammo556", 30, 80),
        arma("ak47", TR, 4, 2, "AK-47", 2700, "ak47", "ammo762", 30, 90),
        arma("m4a4", CT, 4, 3, "M4A4", 2900, "m4a1", "ammo556", 30, 90),
        arma("ssg08", TR, 4, 3, "SSG 08", 1700, "c14", "ammo338lapua", 5, 90),
        arma("famas", CT, 4, 4, "FAMAS", 1950, "g36c", "ammo556", 30, 90),
        arma("sg553", TR, 4, 4, "SG 553", 3000, "aug", "ammo556", 30, 90),
        arma("awp", null, 4, 5, "AWP", 4750, "l96a1", "ammo338lapua", 5, 30, KillCategory.AWP),

        emBreve("flash", null, 5, 1, "Granada de Luz", 200),
        emBreve("fumaca", null, 5, 2, "Fumaça", 300),
        ShopEntry(
            "he", null, 5, 3, "Granada Explosiva", 300, EntryKind.GRANADA,
            item = "pointblank:grenade", icone = "pointblank:textures/item/grenade.icon.png",
        ),
        emBreve("incendiaria", CT, 5, 4, "Incendiária", 500),
        emBreve("molotov", TR, 5, 4, "Molotov", 400),
        emBreve("decoy", null, 5, 5, "Distração", 50),
    )

    fun starterKey(team: Team): String = if (team == Team.CT) "usp_s" else "glock18"
}

object ShopValidation {
    /** Problemas que impedem usar a loja (mensagens em português, uma por problema). */
    fun problems(entries: List<ShopEntry>): List<String> {
        val out = mutableListOf<String>()
        for (t in Team.entries) {
            val loja = entries.forTeam(t)
            loja.groupBy { it.chave }.filter { it.value.size > 1 }.keys.forEach { out += "chave repetida no $t: $it" }
            loja.groupBy { it.coluna to it.posicao }.filter { it.value.size > 1 }
                .forEach { (pos, es) -> out += "posição ${pos.first},${pos.second} repetida no $t: ${es.joinToString { it.chave }}" }
        }
        for (e in entries) {
            if (e.preco < 0) out += "${e.chave}: preço negativo"
            if (e.coluna !in 1..5 || e.posicao !in 1..5) out += "${e.chave}: coluna/posição fora de 1..5"
            val arma = e.tipo == EntryKind.PRIMARIA || e.tipo == EntryKind.PISTOLA
            if ((arma || e.tipo == EntryKind.GRANADA) && e.item.isNullOrBlank()) out += "${e.chave}: falta o item"
            if (arma && (e.municao.isNullOrBlank() || e.pente <= 0)) out += "${e.chave}: falta munição ou pente"
        }
        return out.distinct()
    }
}

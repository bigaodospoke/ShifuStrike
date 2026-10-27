package com.shifustrike.arena

import com.shifustrike.core.Box

data class Ponto(val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float)

/** O mapa do evento: spawns de cada time, os dois bombsites, a área inteira e o ponto de saída. */
class ArenaData {
    var dimensao: String = "minecraft:overworld"
    var spawnsCt: MutableList<Ponto> = mutableListOf()
    var spawnsTr: MutableList<Ponto> = mutableListOf()
    var bombaA: Box? = null
    var bombaB: Box? = null
    var area: Box? = null
    var saida: Ponto? = null

    val empty: Boolean get() = spawnsCt.isEmpty() && spawnsTr.isEmpty() && bombaA == null && bombaB == null && area == null && saida == null

    fun problemas(): List<String> = buildList {
        if (spawnsCt.isEmpty()) add("falta spawn CT (/cs mapa spawn ct)")
        if (spawnsTr.isEmpty()) add("falta spawn TR (/cs mapa spawn tr)")
        if (bombaA == null) add("falta bomba A (/cs mapa bomba a <de> <até>)")
        if (bombaB == null) add("falta bomba B (/cs mapa bomba b <de> <até>)")
        if (area == null) add("falta área do mapa (/cs mapa area <de> <até>)")
        if (saida == null) add("falta saída (/cs mapa saida)")
        val a = area ?: return@buildList
        (spawnsCt + spawnsTr).filterNot { a.contains(it.x, it.y, it.z) }.forEach { add("spawn fora da área: ${it.x.toInt()} ${it.y.toInt()} ${it.z.toInt()}") }
        saida?.let { if (a.contains(it.x, it.y, it.z)) add("a saída está dentro da área do mapa") }
    }
}

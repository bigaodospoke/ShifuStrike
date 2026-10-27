package com.shifustrike.core

import kotlin.math.roundToInt

data class ArmorResult(val healthDamage: Float, val armorLeft: Int)

object ArmorMath {
    /**
     * Colete do CS, em pontos do CS (vida 100). O colete segura parte do dano (`ratio` é o que passa para a vida)
     * e gasta `bonus` ponto de armadura por ponto segurado. Tiro na cabeça só é protegido com capacete.
     */
    fun apply(damage: Float, armor: Int, helmet: Boolean, headshot: Boolean, ratio: Float = 0.5f, bonus: Float = 0.5f): ArmorResult {
        if (armor <= 0 || (headshot && !helmet)) return ArmorResult(damage, armor)
        var health = damage * ratio
        var armorDamage = (damage - health) * bonus
        if (armorDamage > armor) {
            health = damage - armor / bonus
            armorDamage = armor.toFloat()
        }
        return ArmorResult(health, (armor - armorDamage).roundToInt().coerceAtLeast(0))
    }
}

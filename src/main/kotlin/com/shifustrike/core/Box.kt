package com.shifustrike.core

/** Caixa de blocos com os dois cantos inclusos (o bloco do canto máximo conta inteiro). */
data class Box(val minX: Int, val minY: Int, val minZ: Int, val maxX: Int, val maxY: Int, val maxZ: Int) {
    fun contains(x: Double, y: Double, z: Double): Boolean =
        x >= minX && x < maxX + 1.0 && y >= minY && y < maxY + 1.0 && z >= minZ && z < maxZ + 1.0

    companion object {
        fun of(ax: Int, ay: Int, az: Int, bx: Int, by: Int, bz: Int) =
            Box(minOf(ax, bx), minOf(ay, by), minOf(az, bz), maxOf(ax, bx), maxOf(ay, by), maxOf(az, bz))
    }
}

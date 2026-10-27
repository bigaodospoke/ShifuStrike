package com.shifustrike.core

import kotlin.random.Random

object TeamBalancer {
    /** Embaralha e alterna os times; a diferença entre eles nunca passa de 1. */
    fun <T> split(ids: List<T>, random: Random): Map<T, Team> {
        val first = if (random.nextBoolean()) Team.CT else Team.TR
        return ids.shuffled(random).withIndex().associate { (i, id) -> id to if (i % 2 == 0) first else first.other() }
    }
}

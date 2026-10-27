package com.shifustrike.core

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TeamBalancerTest {
    @Test
    fun `times equilibrados`() {
        for (n in 1..11) {
            val ids = (1..n).map { "j$it" }
            val times = TeamBalancer.split(ids, Random(n))
            assertEquals(ids.toSet(), times.keys)
            val ct = times.values.count { it == Team.CT }
            val tr = times.values.count { it == Team.TR }
            assertTrue(abs(ct - tr) <= 1, "n=$n ct=$ct tr=$tr")
        }
    }

    @Test
    fun `mesma semente mesma divisao`() {
        val ids = (1..8).map { "j$it" }
        assertEquals(TeamBalancer.split(ids, Random(42)), TeamBalancer.split(ids, Random(42)))
    }
}

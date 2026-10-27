package com.shifustrike

import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun `id do mod`() = assertEquals("shifustrike:x", ShifuStrike.id("x").toString())
}

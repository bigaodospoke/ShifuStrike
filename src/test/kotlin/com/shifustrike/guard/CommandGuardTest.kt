package com.shifustrike.guard

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommandGuardTest {
    private val permitidos = listOf("cs", "msg", "tell", "w", "r")

    @Test
    fun `comandos de conversa e do evento passam`() {
        assertTrue(CommandGuard.isAllowed("/cs sair", permitidos))
        assertTrue(CommandGuard.isAllowed("msg Fulano oi", permitidos))
        assertTrue(CommandGuard.isAllowed("/minecraft:tell a b", permitidos))
        assertTrue(CommandGuard.isAllowed("/CS status", permitidos))
    }

    @Test
    fun `teleportes e o resto ficam bloqueados`() {
        assertFalse(CommandGuard.isAllowed("/home", permitidos))
        assertFalse(CommandGuard.isAllowed("/tpa x", permitidos))
        assertFalse(CommandGuard.isAllowed("spawn", permitidos))
        assertFalse(CommandGuard.isAllowed("", permitidos))
        assertFalse(CommandGuard.isAllowed("/", permitidos))
    }
}

package com.shifustrike.pointblank

/**
 * Se o último tiro do PointBlank neste thread foi na cabeça. O mixin no HurtingItem grava logo antes
 * do dano; quem trata o dano lê e limpa.
 */
object HeadshotFlag {
    private val flag = ThreadLocal.withInitial { false }

    fun set(v: Boolean) = flag.set(v)

    fun consume(): Boolean {
        val v = flag.get()
        flag.set(false)
        return v
    }

    fun clear() = flag.set(false)
}

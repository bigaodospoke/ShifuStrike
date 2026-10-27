package com.shifustrike.core

/** CT = Antiterroristas, TR = Terroristas. */
enum class Team {
    CT, TR;

    fun other(): Team = if (this == CT) TR else CT
}

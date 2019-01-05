package com.vchernogorov.discordbot.mapper

import org.jetbrains.exposed.sql.ResultRow

interface BipolarMapper<T> {
    fun map(value: T): String

    fun map(value: String): T

    fun map(resultRows: List<ResultRow>): T
}
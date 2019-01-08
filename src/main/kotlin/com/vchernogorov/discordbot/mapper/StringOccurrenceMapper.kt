package com.vchernogorov.discordbot.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vchernogorov.discordbot.UserMessage
import net.dv8tion.jda.core.entities.Guild
import org.jetbrains.exposed.sql.ResultRow
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class StringOccurrenceMapper(val gson: Gson, val guild: Guild, val strings: List<List<String>>) : BipolarMapper<Map<String, Long>> {
    
    override fun map(value: Map<String, Long>): String {
        return gson.toJson(value)
    }

    override fun map(value: String): Map<String, Long> {
        val type = object : TypeToken<Map<String, Long>>() {}.type
        val fromJson = gson.fromJson<Map<String, Long>>(value, type)
        return fromJson
    }

    override fun map(resultRows: List<ResultRow>): Map<String, Long> {
        val botIds = guild.members.filter { it.user.isBot }.map { it.user.id }
        return resultRows.filter {
            it[UserMessage.content].isNotEmpty() && 
                    it[UserMessage.creatorId] != null && 
                    !botIds.contains(it[UserMessage.creatorId])
        }.map {
            val count = strings.map { strings2 ->
                strings2.map { string -> it[UserMessage.content].split(string).size - 1 }.min() ?: 0
            }.sum()
            it[UserMessage.creatorId]!! to count
        }.groupingBy { it.first }.aggregate { _, accumulator, element, _ ->
            (accumulator ?: 0) + element.second
        }
    }
}
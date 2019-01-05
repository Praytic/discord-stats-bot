package com.vchernogorov.discordbot.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vchernogorov.discordbot.UserMessage
import net.dv8tion.jda.core.entities.Guild
import org.jetbrains.exposed.sql.ResultRow
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class TopEmoteDailyUsageStatsMapper(val gson: Gson, val guild: Guild) : BipolarMapper<List<Triple<String, String, DateTime>>> {

    private val dateformatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    override fun map(value: List<Triple<String, String, DateTime>>): String {
        val result = value.map {
            Triple(it.first, it.second, it.third.toString(dateformatter))
        }
        return gson.toJson(result)
    }

    override fun map(value: String): List<Triple<String, String, DateTime>> {
        val type = object : TypeToken<List<Triple<String, String, String>>>() {}.type
        val fromJson = gson.fromJson<List<Triple<String, String, String>>>(value, type)
        return fromJson.map {
            Triple(it.first, it.second, dateformatter.parseDateTime(it.third))
        }
    }

    override fun map(resultRows: List<ResultRow>): List<Triple<String, String, DateTime>> {
        val emoteRegex = "<:(.*?):[0-9]{18}>".toRegex()
        val botIds = guild.members.filter { it.user.isBot }.map { it.user.id }
        return resultRows.map {
            Triple(
                    it[UserMessage.creatorId],
                    emoteRegex.findAll(it[UserMessage.content], 0).toList(),
                    it[UserMessage.creationDate]
            )
        }.filter { (creatorId, emotes, _) ->
            emotes.isNotEmpty() && creatorId != null && !botIds.contains(creatorId)
        }.map { (creatorId, emotes, creationDate) ->
            val emoteIds = emotes.map { it.value.dropLast(1).takeLast(18) }
            emoteIds.map { Triple(creatorId!!, it, creationDate) }
        }.flatten()
    }
}
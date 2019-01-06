package com.vchernogorov.discordbot.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vchernogorov.discordbot.UserMessage
import com.vchernogorov.discordbot.MemberStat
import net.dv8tion.jda.core.entities.Member
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

class MemberStatsMapper(val gson: Gson, val member: Member) : BipolarMapper<MemberStat> {

    override fun map(resultRows: List<ResultRow>): MemberStat {
        val messages = resultRows.count()
        val averageMessagesPerActiveDay = resultRows
                .groupingBy { it[UserMessage.creationDate] }
                .eachCount()
                .values
                .average()
        val activeDays = resultRows.map {
            LocalDate.from(Instant.ofEpochMilli(it[UserMessage.creationDate].millis).atZone(ZoneId.systemDefault()))
        }.toSet()
        val startDate = activeDays.min() ?: throw Exception("StartDate not found during MemberStat mapping")
        val endDate = activeDays.max() ?: throw Exception("EndDate not found during MemberStat mapping")
        val activePeriod = Period.between(startDate, endDate)
        val daysInGuild = (activePeriod.years * 365 + activePeriod.months * 30.42 + activePeriod.days).toInt()
        val messagesCountByDay = resultRows
                .groupingBy { it[UserMessage.creationDate] }
                .eachCount()
        val averageMessagesPerDay = messagesCountByDay.values.sum() * 1.0 / daysInGuild
        val emoteRegex = "<:(.*?):[0-9]{18}>".toRegex()
        val averageMessageLength = resultRows.map {
            it[UserMessage.content].replace(emoteRegex, "").count()
        }.average()
        return MemberStat(
                member.user.name,
                activePeriod,
                daysInGuild,
                activeDays.toSet(),
                messages,
                averageMessageLength,
                averageMessagesPerActiveDay,
                averageMessagesPerDay,
                member.joinDate)
    }

    override fun map(value: MemberStat): String {
        return gson.toJson(value)
    }

    override fun map(value: String): MemberStat {
        val type = object : TypeToken<MemberStat>() {}.type
        val fromJson = gson.fromJson<MemberStat>(value, type)
        return fromJson
    }
}
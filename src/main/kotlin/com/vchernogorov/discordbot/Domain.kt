package com.vchernogorov.discordbot

import org.jetbrains.exposed.sql.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period

data class UserStat(
        val user: String,
        val activePeriod: Period,
        val activeDays: List<LocalDate>,
        val messages: Int,
        val messageLengthAvg: Double,
        val messagesPerActiveDay: Double,
        val messagesPerDay: Double,
        val joinDate: OffsetDateTime,
        val stringOccurrence: Map<String, Int>
)

object UserMessage : Table() {
    val id = varchar("id", 20).primaryKey()
    val channelId = varchar("channel_id", 20)
    val content = varchar("content", 2000, collate = "utf8_general_ci")
    val creatorId = varchar("creator_id", 20).nullable()
    val creationDate = varchar("creation_date", 26)
}

enum class Mode {
    CHANNEL_STATS,
    USER_STATS,
    PRINT_STATS,
    EMOTE_STATS,
    TOP_EMOTE_USAGE_STATS,
    TOP_USED_EMOTES_BY_USER,
    TOP_USED_EMOTES_BY_USERS,
    TOP_EMOTE_DAILY_USAGE_STATS,
    UNDEFINED
}

enum class StatType {
    MSG,
    MSG_AVG,
    MSG_AVG_ACT,
    MSG_LEN_AVG,
    JOIN_DATE,
    STR_OCCR,
    ACT_PRD,
    ACT_DAYS,
}
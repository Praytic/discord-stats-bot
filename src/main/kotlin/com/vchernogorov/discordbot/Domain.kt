package com.vchernogorov.discordbot

import org.jetbrains.exposed.sql.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period

data class MemberStat(
        val user: String,
        val activePeriod: Period,
        val daysInGuild: Int,
        val activeDays: Set<LocalDate>,
        val messages: Int,
        val messageLengthAvg: Double,
        val messagesPerActiveDay: Double,
        val messagesPerDay: Double,
        val joinDate: OffsetDateTime
)

object TempId : Table() {
    val id = varchar("id", 20)
}

object UserMessage : Table() {
    val id = varchar("id", 20).primaryKey()
    val channelId = varchar("channel_id", 20)
    val content = varchar("content", 2000, collate = "utf8_general_ci")
    val creatorId = varchar("creator_id", 20).nullable()
    val creationDate = datetime("creation_date")
}

enum class Mode {
    CHANNEL_STATS,
    MEMBER_STATS,
    PRINT_STATS,
    EMOTE_STATS,
    TOP_EMOTE_USAGE_STATS,
    TOP_USED_EMOTES_BY_USER,
    TOP_USED_EMOTES_BY_USERS,
    GUILD_AVG_EMOTE_USAGE,
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
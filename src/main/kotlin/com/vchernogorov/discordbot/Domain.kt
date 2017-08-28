package com.vchernogorov.discordbot

import net.dv8tion.jda.core.entities.Message
import java.time.OffsetDateTime

data class UserStat(
    val user: String,
    val messages: Int,
    val messageLengthAvg: Double,
    val messagesPerActiveDay: Double,
    val messagesPerDay: Double,
    val joinDate: OffsetDateTime,
    val stringOccurrence: Map<String, Int>
)

data class UserMessage(
    val id: String,
    val channelId: String,
    val content: String,
    val creatorId: String?,
    val creationDate: OffsetDateTime?
) {
  constructor(message: Message) :
      this(message.id, message.channel.id, message.contentRaw, message.member?.user?.id, message.creationTime)
}

enum class Mode {
  MESSAGES_STATS,
  USERS_STATS,
  GATHER_STATS,
  USER_STATS,
  UNDEFINED
}
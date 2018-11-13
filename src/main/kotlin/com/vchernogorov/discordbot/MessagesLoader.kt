package com.vchernogorov.discordbot

import net.dv8tion.jda.core.entities.Message
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

fun uploadMessages(elements: Collection<Message>) = transaction {
  UserMessage.batchInsert(elements, ignore = true) {
    this[UserMessage.id] = it.id
    this[UserMessage.channelId] = it.channel.id
    this[UserMessage.creationDate] = it.creationTime.toString()
    this[UserMessage.creatorId] = it.author.id
    this[UserMessage.content] = it.contentRaw
  }
}

/**
 * Returns the earliest message saved to the database for all channels with [channelIds].
 */
fun firstSavedMessages(channelIds: List<String>) = transaction {
  UserMessage.select {
    UserMessage.channelId.inList(channelIds)
  }.groupBy {
    it[UserMessage.channelId]
  }.map {
    it.key to it.value.minBy {
      OffsetDateTime.parse(it[UserMessage.creationDate])
    }
  }.toMap()
}

/**
 * Returns the latest message saved to the database for all channels with [channelIds].
 */
fun latestSavedMessages(channelIds: List<String>) = transaction {
  UserMessage.select {
    UserMessage.channelId.inList(channelIds)
  }.groupBy {
    it[UserMessage.channelId]
  }.map {
    it.key to it.value.maxBy {
      OffsetDateTime.parse(it[UserMessage.creationDate])
    }
  }.toMap()
}
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

fun lastSavedMessage(channelId: String) = transaction {
  UserMessage.select {
    UserMessage.channelId.eq(channelId)
  }.minBy {
    try {
      OffsetDateTime.parse(it[UserMessage.creationDate])
    } catch (e: ClassCastException) {
      e.printStackTrace()
      OffsetDateTime.parse(it[UserMessage.creationDate])
    }
  }
}
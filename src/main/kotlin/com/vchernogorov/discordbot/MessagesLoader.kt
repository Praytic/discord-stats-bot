package com.vchernogorov.discordbot

import net.dv8tion.jda.core.entities.Message
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

fun uploadMessages(elements: Iterable<Message>) = transaction {
  UserMessage.batchInsert(elements, ignore = true) {
    this[UserMessage.id] = it.id
    this[UserMessage.channelId] = it.channel.id
    this[UserMessage.creationDate] = DateTime.parse(it.creationTime.toString())
    this[UserMessage.creatorId] = it.author.id
    this[UserMessage.content] = it.contentRaw
  }
}

fun lastSavedMessage(channelId: String) = transaction {
  UserMessage.select {
    UserMessage.channelId.eq(channelId)
  }.minBy { it[UserMessage.creationDate] }
}
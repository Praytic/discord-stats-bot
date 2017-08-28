package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.UserMessage
import com.vchernogorov.discordbot.fromJson
import com.vchernogorov.discordbot.gson
import com.vchernogorov.discordbot.send
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.io.File
import java.time.LocalDate
import java.time.Period

abstract class MessagesStatsTask : MessageTask() {

  val statsFolderName = "stats"

  protected fun totalMessages(messages: List<UserMessage>) = messages.count()

  protected fun avgMessagesPerActiveDay(messages: List<UserMessage>): Double {
    val firstMessageDate = messages.minBy { it.creationDate?.toEpochSecond() ?: Long.MAX_VALUE }?.creationDate
    val lastMessageDate = messages.maxBy { it.creationDate?.toEpochSecond() ?: Long.MIN_VALUE }?.creationDate
    val period = Period.between(
        firstMessageDate?.toLocalDate() ?: LocalDate.now(),
        lastMessageDate?.toLocalDate() ?: LocalDate.now())
    val avgMessages = if (period.isZero) 0.0 else totalMessages(messages) / (period.toTotalMonths() * 30.42 + period.days)
    return avgMessages
  }

  protected fun avgMessagesPerDay(messages: List<UserMessage>): Double {
    val firstMessageDate = messages.minBy { it.creationDate?.toEpochSecond() ?: Long.MAX_VALUE }?.creationDate
    val period = Period.between(firstMessageDate?.toLocalDate() ?: LocalDate.now(), LocalDate.now())
    val avgMessages = if (period.isZero) 0.0 else totalMessages(messages) / (period.toTotalMonths() * 30.42 + period.days)
    return avgMessages
  }


  protected fun totalOccurrences(messages: List<UserMessage>, vararg inputs: String): MutableMap<String, Int> {
    val stringOccurrences = mutableMapOf<String, Int>()
    inputs.forEach { input ->
      stringOccurrences.put(input, messages.count { it.content.contains(input, true) })
    }
    return stringOccurrences
  }

  protected fun avgMessageLength(messages: List<UserMessage>): Double {
    val avg = messages.map { it.content.length }.average()
    return if (avg.isNaN()) 0.0 else avg
  }
}

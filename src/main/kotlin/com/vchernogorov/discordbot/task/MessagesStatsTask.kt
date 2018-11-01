package com.vchernogorov.discordbot.task

abstract class MessagesStatsTask : MessageTask() {

  val statsFolderName = "stats"
//
//  protected fun totalMessages(messages: Collection<UserMessage>) = messages.count()
//
//  protected fun activeDays(messages: Collection<UserMessage>): List<LocalDate> {
//    return messages.groupBy { it.creationDate }.keys.mapNotNull { it?.toLocalDate() }
//  }
//
//  protected fun activePeriod(messages: Collection<UserMessage>): Period {
//    val firstMessageDate = messages.minBy { it.creationDate?.toEpochSecond() ?: Long.MAX_VALUE }?.creationDate
//    val lastMessageDate = messages.maxBy { it.creationDate?.toEpochSecond() ?: Long.MIN_VALUE }?.creationDate
//    return Period.between(
//        firstMessageDate?.toLocalDate() ?: LocalDate.now(),
//        lastMessageDate?.toLocalDate() ?: LocalDate.now())
//  }
//
//  protected fun avgMessagesPerActiveDay(messages: Collection<UserMessage>): Double {
//    val activeDays = activeDays(messages).size
//    return if (activeDays == 0) 0.0 else totalMessages(messages) * 1.0 / activeDays
//  }
//
//  protected fun avgMessagesPerDay(messages: Collection<UserMessage>): Double {
//    val period = activePeriod(messages)
//    return if (period.isZero) 0.0 else totalMessages(messages) / (period.toTotalMonths() * 30.42 + period.days)
//  }
//
//  protected fun totalOccurrences(messages: Collection<UserMessage>, vararg inputs: String): MutableMap<String, Int> {
//    val stringOccurrences = mutableMapOf<String, Int>()
//    inputs.forEach { input ->
//      stringOccurrences.put(input, messages.count { it.content.contains(input, true) })
//    }
//    return stringOccurrences
//  }
//
//  protected fun avgMessageLength(messages: Collection<UserMessage>): Double {
//    val avg = messages.map { it.content.length }.average()
//    return if (avg.isNaN()) 0.0 else avg
//  }
}

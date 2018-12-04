package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.TransactionsManager
import com.vchernogorov.discordbot.UserStatsArgs
import com.vchernogorov.discordbot.send
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.joda.time.DateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class GuildAverageUsageEmoteStatsTask(val transactionsManager: TransactionsManager) : MessagesStatsTask() {

    override fun execute(event: MessageReceivedEvent, args: UserStatsArgs) {
        val emotesUsed = transactionsManager.selectEmotesByCreatorsAndCreationDate(event.guild, args)
        val minCreationDateByEmote = minCreationDateByEmote(event.jda, emotesUsed)
        val sortedEmotesUsed = sortEmotesByUsageRate(event.jda, emotesUsed, minCreationDateByEmote)
        val messageBuilder = generateResponseMessage(sortedEmotesUsed, args)
        event.send(messageBuilder)
    }

    fun generateResponseMessage(emotesUsed: List<Triple<Emote, Int, Double>>, args: UserStatsArgs): MessageBuilder {
        val messageBuilder = MessageBuilder().append("[Emote average usage per day in guild]\n")
        emotesUsed.forEachIndexed { i, (emote, count, usageRate) ->
            if (args.tail && emotesUsed.count() - args.limitPrimaryResults <= i ||
                    !args.tail && args.limitPrimaryResults > i) {
                messageBuilder
                        .append("${i + 1}. ")
                        .append(emote)
                        .append(": `%.2f | $count`\n".format(usageRate))
            }
        }
        return messageBuilder
    }

    private fun minCreationDateByEmote(jda: JDA, emotesUsed: List<Triple<String, String, DateTime>>): Map<String, DateTime?> {
        return emotesUsed.distinctBy { (_, emote, _) ->
            emote
        }.map { (_, emote, _) ->
            emote to emotesUsed.filter {
                it.second == emote
            }.map { (_, _, creationDate) ->
                creationDate
            }.min()
        }.toMap()
    }

    private fun sortEmotesByUsageRate(jda: JDA,
                                      emotesUsed: List<Triple<String, String, DateTime>>,
                                      minCreationDateByEmote: Map<String, DateTime?>): List<Triple<Emote, Int, Double>> {
        return emotesUsed.groupingBy {
            it.second
        }.eachCount().map { (emote, count) ->
            val daysLive = ChronoUnit.DAYS.between(LocalDateTime.parse(minCreationDateByEmote[emote]?.toString()), LocalDate.now())
            Triple(emote, count, count * 1.0 / daysLive)
        }.map { (emote, count, usageRate) ->
            Triple(jda.getEmoteById(emote), count, usageRate)
        }.filter { (emote) ->
            emote != null
        }.sortedByDescending { (_, _, usageRate) ->
            usageRate
        }
    }
}
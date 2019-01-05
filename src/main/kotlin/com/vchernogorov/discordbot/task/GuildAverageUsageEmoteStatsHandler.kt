package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.manager.TransactionsManager
import com.vchernogorov.discordbot.args.UserStatsArgs
import com.vchernogorov.discordbot.send
import mu.KotlinLogging
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.joda.time.DateTime
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Shows how much each emote has been used during specified period or from the creation time.
 */
class GuildAverageUsageEmoteStatsHandler(val transactionsManager: TransactionsManager) : AbstractCommandHandler() {

    override val logger = KotlinLogging.logger {}

    override fun handle(event: MessageReceivedEvent, args: UserStatsArgs) {
        logger.debug { "Selecting emotes, creators and creation dates for guild: ${event.guild}." }
        val emotesUsed = transactionsManager.selectEmotesByCreatorsAndCreationDate(event.guild, args)

        logger.debug { "Gathering the first creation date for each emote for guild: ${event.guild}." }
        val minCreationDateByEmote = minCreationDateByEmote(event.jda, emotesUsed)

        logger.debug { "Sorting emotes by usage rate for guild: ${event.guild}." }
        val sortedEmotesUsed = sortEmotesByUsageRate(event.jda, emotesUsed, minCreationDateByEmote)

        logger.debug { "Generating discord message with the results for guild: ${event.guild}." }
        val messageBuilder = generateResponseMessage(sortedEmotesUsed, args).stringBuilder

        event.send(messageBuilder)
    }

    private fun generateResponseMessage(emotesUsed: List<Triple<Emote, Int, Double>>, args: UserStatsArgs): MessageBuilder {
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

    private fun sortEmotesByUsageRate(
            jda: JDA,
            emotesUsed: List<Triple<String, String, DateTime>>,
            minCreationDateByEmote: Map<String, DateTime?>): List<Triple<Emote, Int, Double>> {
        return emotesUsed.groupingBy {
            it.second
        }.eachCount().map { (emote, count) ->
            val epochCreationTime = minCreationDateByEmote[emote]?.toInstant()?.millis
            val firstTimePosted = if (epochCreationTime != null) {
                Instant.ofEpochMilli(epochCreationTime)
            } else {
                logger.warn { "Emote $emote doesn't have creation time." }
                Instant.now()
            }
            val daysLive = ChronoUnit.DAYS.between(firstTimePosted, Instant.now())
            Triple(emote, count, count * 1.0 / daysLive)
        }.map { (emote, count, usageRate) ->
            Triple(jda.getEmoteById(emote), count, usageRate)
        }.filter { (emote, _, usageRate) ->
            emote != null && usageRate.isFinite()
        }.sortedByDescending { (_, _, usageRate) ->
            usageRate
        }
    }
}
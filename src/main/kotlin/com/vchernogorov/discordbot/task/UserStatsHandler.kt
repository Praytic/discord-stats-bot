package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.UserStat
import com.vchernogorov.discordbot.args.UserStatsArgs
import com.vchernogorov.discordbot.manager.TransactionsManager
import com.vchernogorov.discordbot.send
import mu.KotlinLogging
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.format.DateTimeFormatter

/**
 * Shows generic statistics for the specified user, like: how much messages did user created, how often he messaged,
 * average symbols posted in each message, etc.
 * This handler is not affected by [UserStatsArgs] because it returns one object.
 */
class UserStatsHandler(val transactionsManager: TransactionsManager) : AbstractCommandHandler() {

    override val logger = KotlinLogging.logger {}

    override fun handle(event: MessageReceivedEvent, args: UserStatsArgs) {
        logger.debug { "Selecting user stat for member: ${event.member}." }
        val userStat = transactionsManager.selectUserStat(event.member, args)

        logger.debug { "Generating discord message with the results for member: ${event.member}." }
        val messageBuilder = generateResponseMessage(userStat).stringBuilder

        event.send(messageBuilder)
    }

    private fun generateResponseMessage(userStat: UserStat): MessageBuilder {
        val messageBuilder = MessageBuilder()
                .append("`[User statistics for ${userStat.user}]`\n")
                .append("**Join date:** ${userStat.joinDate.format(DateTimeFormatter.ISO_DATE)}\n")
                .append("**Total messages:** ${userStat.messages} messages\n")
                .append("**Average message length:** ${userStat.messageLengthAvg.toInt()} symbols\n")
                .append("**Messages per day:** ${userStat.messagesPerDay.toInt()} messages\n")
                .append("**Messages per active day:** ${userStat.messagesPerActiveDay.toInt()} messages\n")
                .append("**Active days:** ${userStat.activeDays.size} days\n")
                .append("**Days in guild:** ${userStat.daysInGuild} days\n")
        return messageBuilder
    }
}
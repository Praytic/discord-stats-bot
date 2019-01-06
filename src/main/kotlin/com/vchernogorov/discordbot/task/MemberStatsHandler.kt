package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.Mode
import com.vchernogorov.discordbot.MemberStat
import com.vchernogorov.discordbot.args.MemberStatsArgs
import com.vchernogorov.discordbot.manager.TransactionsManager
import com.vchernogorov.discordbot.send
import com.xenomachina.argparser.ArgParser
import mu.KotlinLogging
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.format.DateTimeFormatter

/**
 * Shows generic statistics for the specified user, like: how much messages user created, how much he creates daily,
 * average symbols posted in each message, etc.
 */
class MemberStatsHandler(val transactionsManager: TransactionsManager) : CommandHandler<MemberStatsArgs> {

    val logger = KotlinLogging.logger {}

    override fun handle(event: MessageReceivedEvent) {
        val mode: Mode = try {
            Mode.valueOf(event.message.contentRaw)
        } catch (e: IllegalArgumentException) {
            Mode.UNDEFINED
        }
        handle(event, ArgParser(arrayOf()).parseInto { MemberStatsArgs(it, event.member, mode) })
    }

    override fun handle(event: MessageReceivedEvent, args: MemberStatsArgs) {
        logger.debug { "Selecting user stat for member: ${event.member}." }
        val userStat = transactionsManager.selectMemberStat(event.member, args)

        logger.debug { "Generating discord message with the results for member: ${event.member}." }
        val messageBuilder = generateResponseMessage(userStat).stringBuilder

        event.send(messageBuilder)
    }

    private fun generateResponseMessage(memberStat: MemberStat): MessageBuilder {
        val messageBuilder = MessageBuilder()
                .append("`[User statistics for ${memberStat.user}]`\n")
                .append("**Join date:** ${memberStat.joinDate.format(DateTimeFormatter.ISO_DATE)}\n")
                .append("**Total messages:** ${memberStat.messages} messages\n")
                .append("**Average message length:** ${memberStat.messageLengthAvg.toInt()} symbols\n")
                .append("**Messages per day:** ${memberStat.messagesPerDay.toInt()} messages\n")
                .append("**Messages per active day:** ${memberStat.messagesPerActiveDay.toInt()} messages\n")
                .append("**Active days:** ${memberStat.activeDays.size} days\n")
                .append("**Days in guild:** ${memberStat.daysInGuild} days\n")
        return messageBuilder
    }
}
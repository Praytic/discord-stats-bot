package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.Mode
import com.vchernogorov.discordbot.args.MemberStatsArgs
import com.vchernogorov.discordbot.manager.TransactionsManager
import com.vchernogorov.discordbot.memberStatCompareMessage
import com.vchernogorov.discordbot.memberStatMessage
import com.vchernogorov.discordbot.send
import com.xenomachina.argparser.ArgParser
import mu.KotlinLogging
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

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
        logger.debug { "Selecting stat for member: ${args.member}." }
        val memberStat = transactionsManager.selectMemberStat(args.member, args)

        if (args.compare.isNotEmpty()) {
            logger.debug { "Selecting stats for members: ${args.compare}." }
            val compareStats = args.compare.map { transactionsManager.selectMemberStat(it, args) }

            logger.debug { "Generating discord message with the results for member ${args.member} and comparisons ${args.compare}." }
            val messageBuilder = memberStatCompareMessage(memberStat, compareStats).stringBuilder
            event.send(messageBuilder)
        } else {
            logger.debug { "Generating discord message with the results for member: ${event.member}." }
            val messageBuilder = memberStatMessage(memberStat).stringBuilder
            event.send(messageBuilder)
        }
    }
}
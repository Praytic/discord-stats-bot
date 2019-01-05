package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.Mode
import com.vchernogorov.discordbot.args.UserStatsArgs
import com.vchernogorov.discordbot.manager.TransactionsManager
import com.xenomachina.argparser.ArgParser
import mu.KLogger
import mu.KotlinLogging
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jetbrains.exposed.sql.transactions.TransactionManager

class GenericCommandHandler(transactionsManager: TransactionsManager) : AbstractCommandHandler() {

    override val logger = KotlinLogging.logger {}

    val handlers = mapOf(
            Mode.USER_STATS to UserStatsHandler(transactionsManager),
            Mode.EMOTE_STATS to UserEmoteStatsTask(),
            Mode.TOP_EMOTE_USAGE_STATS to GuildMostUsedEmoteStatsTask(),
            Mode.TOP_USED_EMOTES_BY_USERS to UsersMostUsedEmoteStatsTask(),
            Mode.TOP_EMOTE_DAILY_USAGE_STATS to GuildAverageUsageEmoteStatsHandler(transactionsManager)
    )

    override fun handle(event: MessageReceivedEvent, args: UserStatsArgs) {
        handlers[args.command]?.handle(event, args)
    }
}

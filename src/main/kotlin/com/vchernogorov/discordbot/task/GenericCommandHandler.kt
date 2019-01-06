package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.Mode
import com.vchernogorov.discordbot.args.GuildStatsArgs
import com.vchernogorov.discordbot.args.MemberStatsArgs
import com.vchernogorov.discordbot.args.StatsArgs
import com.vchernogorov.discordbot.manager.TransactionsManager
import com.xenomachina.argparser.ArgParser
import mu.KotlinLogging
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class GenericCommandHandler(transactionsManager: TransactionsManager) : CommandHandler<StatsArgs> {

    val logger = KotlinLogging.logger {}

    val memberStatsHandler = MemberStatsHandler(transactionsManager)
    val guildAverageEmoteUsageStatsHandler = GuildAverageUsageEmoteStatsHandler(transactionsManager)

    override fun handle(event: MessageReceivedEvent) {
        val commands = event.message.contentRaw.split(" ")
        val params = commands.drop(1).toTypedArray()
        val command = commands[0]
        val mode: Mode = try {
            val mode = Mode.valueOf(command)
            logger.info { "Command [$command] with params ${params.joinToString(" ")} was called by [${event.author}]." }
            mode
        } catch (e: IllegalArgumentException) {
            Mode.UNDEFINED
            return
        }

        if (!params.isEmpty()) {
            when (mode) {
                Mode.MEMBER_STATS -> memberStatsHandler
                        .handle(event, ArgParser(params).parseInto { MemberStatsArgs(it, event.member, mode) })
                Mode.GUILD_AVG_EMOTE_USAGE -> guildAverageEmoteUsageStatsHandler
                        .handle(event, ArgParser(params).parseInto { GuildStatsArgs(it, event.guild, mode) })
                else -> throw Exception("Unable to process command $command with params ${params.joinToString(" ")}")
            }
        } else {
            when (mode) {
                Mode.MEMBER_STATS -> memberStatsHandler.handle(event)
                Mode.GUILD_AVG_EMOTE_USAGE -> guildAverageEmoteUsageStatsHandler.handle(event)
                else -> throw Exception("Unable to process command $command without params.")
            }
        }
    }

    override fun handle(event: MessageReceivedEvent, args: StatsArgs) {
        throw NotImplementedError()
    }
}

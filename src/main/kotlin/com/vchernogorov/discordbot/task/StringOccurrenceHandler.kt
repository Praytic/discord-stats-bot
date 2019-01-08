package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.Mode
import com.vchernogorov.discordbot.manager.TransactionsManager
import com.vchernogorov.discordbot.args.StringOccurrenceArgs
import com.vchernogorov.discordbot.send
import com.vchernogorov.discordbot.stringOccurrencesMessage
import com.xenomachina.argparser.ArgParser
import mu.KotlinLogging
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

/**
 * Shows rating of members who has used given string in their messages the most.
 */
class StringOccurrenceHandler(val transactionsManager: TransactionsManager) : CommandHandler<StringOccurrenceArgs> {

    val logger = KotlinLogging.logger {}

    override fun handle(event: MessageReceivedEvent) {
        val mode: Mode = try {
            Mode.valueOf(event.message.contentRaw)
        } catch (e: IllegalArgumentException) {
            Mode.UNDEFINED
        }
        handle(event, ArgParser(arrayOf()).parseInto { StringOccurrenceArgs(it, event.guild, mode) })
    }

    override fun handle(event: MessageReceivedEvent, args: StringOccurrenceArgs) {
        if (args.strings.any { it.any { it.length < 3 } }) {
            throw Exception("Searched strings should be longer than 3 symbols.")
        }

        logger.debug { "Selecting messages with ${args.strings} occurrence for guild: ${event.guild}." }
        val occurrencesByMember = transactionsManager.countStringOccurrencesByMember(event.guild, args).map {
            event.jda.getUserById(it.key).name to it.value
        }.toMap()

        logger.debug { "Generating discord message with the results for guild: ${event.guild}." }
        val messageBuilder = stringOccurrencesMessage(occurrencesByMember, args).stringBuilder

        event.send(messageBuilder)
    }
}
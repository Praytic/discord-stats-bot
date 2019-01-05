package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.Mode
import com.vchernogorov.discordbot.args.UserStatsArgs
import com.xenomachina.argparser.ArgParser
import mu.KLogger
import mu.KotlinLogging
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

abstract class AbstractCommandHandler : CommandHandler {

    abstract val logger: KLogger

    abstract fun handle(event: MessageReceivedEvent, args: UserStatsArgs)

    override fun handle(event: MessageReceivedEvent) {
        val commands = event.message.contentRaw.split(" ")
        val params = commands.drop(1).toTypedArray()
        val command = commands[0]
        val mode: Mode = try {
            val mode = Mode.valueOf(command)
            logger.info { "Command [$command] with params [${params.joinToString(" ")}] was called by [${event.author}]." }
            mode
        } catch (e: IllegalArgumentException) {
            Mode.UNDEFINED
        }
        if (mode != Mode.UNDEFINED) {
            handle(event, ArgParser(params).parseInto { UserStatsArgs(it, event.guild, mode) })
        }
    }
}

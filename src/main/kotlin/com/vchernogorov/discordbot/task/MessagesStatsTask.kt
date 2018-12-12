package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.UserStatsArgs
import com.xenomachina.argparser.ArgParser
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

abstract class MessagesStatsTask : MessageTask {
    abstract fun execute(event: MessageReceivedEvent, args: UserStatsArgs)

    override fun execute(event: MessageReceivedEvent, vararg params: String) {
        execute(event, ArgParser(params).parseInto { UserStatsArgs(it, event.guild) })
    }
}

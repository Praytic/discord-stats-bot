package com.vchernogorov.discordbot.task

import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

abstract class UserMessagesStatsTask : MessagesStatsTask() {

    override fun execute(
            event: MessageReceivedEvent,
            members: List<Member>,
            channels: List<TextChannel>,
            messageExcFilter: List<String>,
            messageIncFilter: List<String>) {
        execute(event,
                members.singleOrNull() ?: throw Exception("One user parameter is required for this request."),
                channels,
                messageExcFilter,
                messageIncFilter)
    }

    abstract fun execute(
            event: MessageReceivedEvent,
            member: Member,
            channels: List<TextChannel> = emptyList(),
            messageExcFilter: List<String> = emptyList(),
            messageIncFilter: List<String> = emptyList())

}

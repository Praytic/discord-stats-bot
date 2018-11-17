package com.vchernogorov.discordbot.task

import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class ChannelStatsTask : MessagesStatsTask() {
    override fun execute(event: MessageReceivedEvent, users: List<Member>, channels: List<TextChannel>, messageExcFilter: List<String>, messageIncFilter: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
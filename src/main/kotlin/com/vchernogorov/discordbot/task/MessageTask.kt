package com.vchernogorov.discordbot.task

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

interface MessageTask {
    fun execute(event: MessageReceivedEvent, vararg params: String)
}
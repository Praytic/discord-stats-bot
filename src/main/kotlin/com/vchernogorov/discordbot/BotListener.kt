package com.vchernogorov.discordbot

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class BotListener : ListenerAdapter() {

    val overwatchHandler = OverwatchCommandHandler()

    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event == null || !event.message.content.startsWith("=")) return
        val command = event.message.content
        val commandRawType = command.split(" ")[0]
        val commandBody = command.removePrefix("$commandRawType ")

        try {
            when (commandRawType) {
                "=ow" -> overwatchHandler.handle(event, commandBody)
                else -> return
            }
        }
        catch (ex: Throwable) {
            event.channel.sendMessage(ex.message).queue()
            ex.printStackTrace()
        }
    }
}
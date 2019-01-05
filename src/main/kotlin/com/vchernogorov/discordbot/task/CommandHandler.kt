package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.Mode
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

/**
 * Represents handler for all message commands sent from discord users.
 */
interface CommandHandler {

    /**
     * Handles execution of specified [event].
     */
    fun handle(event: MessageReceivedEvent)
}
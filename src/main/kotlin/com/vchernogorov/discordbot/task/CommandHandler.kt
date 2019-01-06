package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.Mode
import com.vchernogorov.discordbot.args.StatsArgs
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

/**
 * Represents handler for all message commands sent from discord users.
 */
interface CommandHandler<T : StatsArgs> {

    /**
     * Handles execution of specified [event].
     */
    fun handle(event: MessageReceivedEvent)

    fun handle(event: MessageReceivedEvent, args: T)
}
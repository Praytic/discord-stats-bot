package com.vchernogorov.discordbot

import com.vchernogorov.discordbot.task.ChannelStatsTask
import com.vchernogorov.discordbot.task.UserEmoteStatsTask
import com.vchernogorov.discordbot.task.GuildMostUsedEmoteStatsTask
import com.vchernogorov.discordbot.task.UserMostUsedEmoteStatsTask
import com.vchernogorov.discordbot.task.UserStatsTask
import mu.KotlinLogging
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.utils.PermissionUtil

class OwnerCommandListener(val printErrorsToDiscord: Boolean) : ListenerAdapter() {
    private val logger = KotlinLogging.logger {}

    val tasks = mapOf(
            Mode.CHANNEL_STATS to ChannelStatsTask(),
            Mode.USER_STATS to UserStatsTask(),
            Mode.EMOTE_STATS to UserEmoteStatsTask(),
            Mode.TOP_EMOTE_USAGE_STATS to GuildMostUsedEmoteStatsTask(),
            Mode.TOP_USED_EMOTE_BY_USER to UserMostUsedEmoteStatsTask()
    )

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) {
            return
        }

        val commands = event.message.contentRaw.split(" ")
        val params = commands.drop(1).toTypedArray()
        val command = commands[0]
        val mode: Mode = try {
            Mode.valueOf(command)
        } catch (e: IllegalArgumentException) {
            Mode.UNDEFINED
        }
        try {
            tasks[mode]?.execute(event, *params)
        } catch (e: Exception) {
            if (printErrorsToDiscord) {
                if (PermissionUtil.checkPermission(event.textChannel, event.guild.selfMember, Permission.MESSAGE_WRITE)) {
                    event.send(e.message ?: "Error without message occurred. Check logs.")
                } else {
                    logger.error { "Bot doesn't have ${Permission.MESSAGE_WRITE} permission. Error won't be printed to discord channel." }
                }
            }
            logger.error(e) { "Command $command execution completed with error with parameters $params." }
        }
    }
}
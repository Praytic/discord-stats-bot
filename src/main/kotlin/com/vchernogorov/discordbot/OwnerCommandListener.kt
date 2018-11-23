package com.vchernogorov.discordbot

import com.vchernogorov.discordbot.task.ChannelStatsTask
import com.vchernogorov.discordbot.task.GuildAverageUsageEmoteStatsTask
import com.vchernogorov.discordbot.task.GuildMostUsedEmoteStatsTask
import com.vchernogorov.discordbot.task.UserEmoteStatsTask
import com.vchernogorov.discordbot.task.UserStatsTask
import com.vchernogorov.discordbot.task.UsersMostUsedEmoteStatsTask
import com.xenomachina.argparser.ShowHelpException
import mu.KotlinLogging
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.utils.PermissionUtil
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.nio.charset.StandardCharsets

class OwnerCommandListener(val printErrorsToDiscord: Boolean,
                           val removeOriginalRequest: Boolean,
                           transactionsManager: TransactionsManager) : ListenerAdapter() {
    private val logger = KotlinLogging.logger {}

    val tasks = mapOf(
            Mode.CHANNEL_STATS to ChannelStatsTask(),
            Mode.USER_STATS to UserStatsTask(),
            Mode.EMOTE_STATS to UserEmoteStatsTask(),
            Mode.TOP_EMOTE_USAGE_STATS to GuildMostUsedEmoteStatsTask(),
            Mode.TOP_USED_EMOTES_BY_USERS to UsersMostUsedEmoteStatsTask(),
            Mode.TOP_EMOTE_DAILY_USAGE_STATS to GuildAverageUsageEmoteStatsTask(transactionsManager)
    )

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) {
            return
        }

        logger.debug { "New message request: ${event.message.contentRaw}" }

        val commands = event.message.contentRaw.split(" ")
        val params = commands.drop(1).toTypedArray()
        val command = commands[0]
        val mode: Mode = try {
            Mode.valueOf(command)
        } catch (e: IllegalArgumentException) {
            Mode.UNDEFINED
        }
        try {
            if (removeOriginalRequest) {
                if (PermissionUtil.checkPermission(event.textChannel, event.guild.selfMember, Permission.MESSAGE_MANAGE)) {
                    event.channel.deleteMessageById(event.messageId).queue()
                } else {
                    logger.warn { "Bot doesn't have ${Permission.MESSAGE_MANAGE} permission. Request message won't be deleted." }
                }
            }
            tasks[mode]?.execute(event, *params)
        } catch (se: ShowHelpException) {
            val baos = ByteArrayOutputStream()
            val writer = OutputStreamWriter(baos)
            se.printUserMessage(writer, null, 80)
            writer.flush()
            val splittedHelpMessage = String(baos.toByteArray(), StandardCharsets.UTF_8).split("\n")
            val helpMessage = splittedHelpMessage.reduceUntil({ acc, s -> acc.length + s.length > 2000 }) { acc, s ->
                acc + "\n" + s
            }
            helpMessage.forEach {
                event.send("```$it```")
            }
        } catch (e: Exception) {
            if (printErrorsToDiscord) {
                if (PermissionUtil.checkPermission(event.textChannel, event.guild.selfMember, Permission.MESSAGE_WRITE)) {
                    event.send("`Error: ${e.message ?: "No message. Check logs for details."}`")
                } else {
                    logger.error { "Bot doesn't have ${Permission.MESSAGE_WRITE} permission. Error won't be printed to discord channel." }
                }
            }
            val baos = ByteArrayOutputStream()
            e.printStackTrace(PrintStream(baos, true, "UTF-8"))
            val data = String(baos.toByteArray(), StandardCharsets.UTF_8)
            logger.error { data }
        }
    }
}
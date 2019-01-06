package com.vchernogorov.discordbot.listener

import com.vchernogorov.discordbot.send
import com.vchernogorov.discordbot.task.GenericCommandHandler
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

class MainCommandListener(
        val authorizedUsers: List<String>,
        val printErrorsToDiscord: Boolean,
        val removeOriginalRequest: Boolean,
        val genericCommandHandler: GenericCommandHandler) : ListenerAdapter() {

    private val logger = KotlinLogging.logger {}

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val isAuthorizedUser = authorizedUsers.isEmpty() || authorizedUsers.contains(event.author.id)
        if (event.author.isBot && !isAuthorizedUser) {
            return
        }

        try {
            genericCommandHandler.handle(event)
            if (removeOriginalRequest) {
                if (PermissionUtil.checkPermission(event.textChannel, event.guild.selfMember, Permission.MESSAGE_MANAGE)) {
                    event.channel.deleteMessageById(event.messageId).queue()
                } else {
                    logger.warn { "Bot doesn't have ${Permission.MESSAGE_MANAGE} permission. Request message won't be deleted." }
                }
            }
        } catch (se: ShowHelpException) {
            val baos = ByteArrayOutputStream()
            val writer = OutputStreamWriter(baos)
            se.printUserMessage(writer, null, 80)
            writer.flush()
            event.send(String(baos.toByteArray(), StandardCharsets.UTF_8), true)
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
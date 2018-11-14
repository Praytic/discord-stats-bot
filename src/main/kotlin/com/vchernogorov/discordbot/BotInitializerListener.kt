package com.vchernogorov.discordbot

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.utils.PermissionUtil
import java.time.Instant

class BotInitializerListener(val fetchDelayMillis: Long,
                             val backoffRetryDelay: Long) : ListenerAdapter() {
  private val logger = KotlinLogging.logger {}

  override fun onReady(event: ReadyEvent) {
    val lastMessageByChannel = mutableMapOf<TextChannel, String?>()
    val firstMessageByChannel = mutableMapOf<TextChannel, String?>()

    logger.info("Initializing 'last messages IDs by channel' map.")
    for (guild in event.jda.guilds) {
      // Not working because of GC overhead error
//    val messageIds = latestSavedMessages(guild.textChannels.map { it.id })
      for (channel in guild.textChannels) {
        val messageIds = latestSavedMessages(listOf(channel.id))
        lastMessageByChannel[channel] = messageIds[channel.id]
      }
    }

    logger.info("Initializing 'first messages IDs by channel' map.")
    for (guild in event.jda.guilds) {
      // Not working because of GC overhead error
//    val messageIds = firstSavedMessages(guild.textChannels.map { it.id })
      for (channel in guild.textChannels) {
        val messageIds = firstSavedMessages(listOf(channel.id))
        firstMessageByChannel[channel] = messageIds[channel.id]
      }
    }

    logger.info("Starting main loop for fetching new/old messages from guilds.")
    GlobalScope.launch {
      while (true) {
        delay(fetchDelayMillis)
        for (guild in event.jda.guilds) {
          for (channel in guild.textChannels
              .filter { PermissionUtil.checkPermission(it, it.guild.selfMember, Permission.MESSAGE_READ) }) {
            if (!lastMessageByChannel.containsKey(channel)) {
              lastMessageByChannel[channel] = null
            }
            if (!firstMessageByChannel.containsKey(channel)) {
              firstMessageByChannel[channel] = null
            }

            backoffRetry(name = "${guild.name}/${channel.name}", initialDelay = backoffRetryDelay, factor = 1.0) {
              val oldMessages = uploadOldMessages(channel, lastMessageByChannel)
              val newMessages = uploadNewMessages(channel, firstMessageByChannel)
              val uploadedMessages = oldMessages.await() + newMessages.await()
              if (uploadedMessages > 0) {
                logger.info("[${Instant.now()}] Uploaded ${uploadedMessages} messages for channel ${guild.name}/${channel.name}.")
              }
            }
          }
        }
      }
    }
  }
}
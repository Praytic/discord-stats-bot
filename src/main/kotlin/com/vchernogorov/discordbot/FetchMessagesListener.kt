package com.vchernogorov.discordbot

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.utils.PermissionUtil
import java.time.Instant

class FetchMessagesListener(val fetchDelayMillis: Long,
                            val backoffRetryDelay: Long,
                            val backoffRetryFactor: Double,
                            val transactionsManager: TransactionsManager) : ListenerAdapter() {
    private val logger = KotlinLogging.logger {}

    override fun onReady(event: ReadyEvent) {
        logger.info("Initializing 'last messages IDs by channel' map.")
        val lastMessageByChannel = initLastMessages(event.jda)

        logger.info("Initializing 'first messages IDs by channel' map.")
        val firstMessageByChannel = initFirstMessages(event.jda)

        logger.info("Starting main loop for fetching new/old messages from guilds.")
        launchMessageFetching(event.jda, lastMessageByChannel, firstMessageByChannel)
    }

    private fun initLastMessages(jda: JDA): MutableMap<TextChannel, String?> {
        val lastMessageByChannel = mutableMapOf<TextChannel, String?>()
        for (guild in jda.guilds) {
            val messageIdsByChannelIds = transactionsManager.latestSavedMessages(guild.textChannels)
            for (channel in guild.textChannels) {
                lastMessageByChannel[channel] = messageIdsByChannelIds[channel.id]
            }
        }
        return lastMessageByChannel
    }

    private fun initFirstMessages(jda: JDA): MutableMap<TextChannel, String?> {
        val firstMessageByChannel = mutableMapOf<TextChannel, String?>()
        for (guild in jda.guilds) {
            val messageIdsByChannelIds = transactionsManager.firstSavedMessages(guild.textChannels)
            for (channel in guild.textChannels) {
                firstMessageByChannel[channel] = messageIdsByChannelIds[channel.id]
            }
        }
        return firstMessageByChannel
    }

    private fun launchMessageFetching(jda: JDA,
            lastMessageByChannel: MutableMap<TextChannel, String?>,
            firstMessageByChannel: MutableMap<TextChannel, String?>) = GlobalScope.launch {
        while (true) {
            delay(fetchDelayMillis)
            for (guild in jda.guilds) {
                for (channel in guild.textChannels
                        .filter { PermissionUtil.checkPermission(it, it.guild.selfMember, Permission.MESSAGE_READ) }) {
                    if (!lastMessageByChannel.containsKey(channel)) {
                        lastMessageByChannel[channel] = null
                    }
                    if (!firstMessageByChannel.containsKey(channel)) {
                        firstMessageByChannel[channel] = null
                    }

                    backoffRetry(name = "${guild.name}/${channel.name}", initialDelay = backoffRetryDelay, factor = backoffRetryFactor) {
                        val oldMessages = uploadOldMessages(channel, firstMessageByChannel)
                        val newMessages = uploadNewMessages(channel, lastMessageByChannel)
                        val uploadedMessages = oldMessages.await() + newMessages.await()
                        if (uploadedMessages > 0) {
                            logger.info("[${Instant.now()}] Uploaded $uploadedMessages messages for channel ${guild.name}/${channel.name}.")
                        }
                    }
                }
            }
        }
    }

    private suspend fun uploadNewMessages(
            channel: TextChannel,
            messagesByChannel: MutableMap<TextChannel, String?>) = coroutineScope {
        var latestSavedMessageId = messagesByChannel[channel]
        val latestMessageId = channel.getLatestMessageIdSafe()
        async {
            var newMessagesUploaded = 0
            while (true) {
                val newMessages = channel.getHistoryAfter(
                        if (latestMessageId == null || latestMessageId == latestSavedMessageId) {
                            break
                        } else if (latestSavedMessageId == null) {
                            latestMessageId
                        } else {
                            latestSavedMessageId
                        }, 100).complete().retrievedHistory
                if (newMessages.isNotEmpty()) {
                    transactionsManager.uploadMessages(newMessages)
                    latestSavedMessageId = newMessages.maxBy { it.creationTime }?.id
                    newMessagesUploaded += newMessages.size
                } else {
                    break
                }
            }
            messagesByChannel[channel] = latestSavedMessageId
            newMessagesUploaded
        }
    }

    private suspend fun uploadMessages(
            channel: TextChannel,
            messagesByChannel: MutableMap<TextChannel, String?>) = coroutineScope {
        var latestSavedMessageId = messagesByChannel[channel]
        val latestMessageId = channel.getLatestMessageIdSafe()
        async {
            var newMessagesUploaded = 0
            while (true) {
                val newMessages = channel.getHistoryAfter(
                        if (latestMessageId == null || latestMessageId == latestSavedMessageId) {
                            break
                        } else if (latestSavedMessageId == null) {
                            latestMessageId
                        } else {
                            latestSavedMessageId
                        }, 100).complete().retrievedHistory
                if (newMessages.isNotEmpty()) {
                    transactionsManager.uploadMessages(newMessages)
                    latestSavedMessageId = newMessages.maxBy { it.creationTime }?.id
                    newMessagesUploaded += newMessages.size
                } else {
                    break
                }
            }
            messagesByChannel[channel] = latestSavedMessageId
            newMessagesUploaded
        }
    }


    private suspend fun uploadOldMessages(
            channel: TextChannel,
            messagesByChannel: MutableMap<TextChannel, String?>) = coroutineScope {
        var firstSavedMessageId = messagesByChannel[channel] ?: channel.getLatestMessageIdSafe()
        async {
            var newMessagesUploaded = 0
            while (true) {
                val newMessages = channel.getHistoryBefore(
                        if (firstSavedMessageId == null) {
                            break
                        } else {
                            firstSavedMessageId
                        }, 100).complete().retrievedHistory
                if (newMessages.isEmpty()) {
                    break
                }
                if (newMessages.isNotEmpty()) {
                    transactionsManager.uploadMessages(newMessages)
                    firstSavedMessageId = newMessages.minBy { it.creationTime }?.id
                    newMessagesUploaded += newMessages.size
                }
            }
            messagesByChannel[channel] = firstSavedMessageId
            newMessagesUploaded
        }
    }
}
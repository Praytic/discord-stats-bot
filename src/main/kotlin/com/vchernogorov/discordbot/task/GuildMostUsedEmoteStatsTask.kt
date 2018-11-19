package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.UserMessage
import com.vchernogorov.discordbot.send
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class GuildMostUsedEmoteStatsTask : MessagesStatsTask() {

    override fun execute(
            event: MessageReceivedEvent,
            members: List<Member>,
            channels: List<TextChannel>,
            messageExcFilter: List<String>,
            messageIncFilter: List<String>) {
        val emotesUsed = transaction {
            val emoteRegex = "<:(.*?):[0-9]{18}>".toRegex()
            val result = UserMessage.slice(UserMessage.content, UserMessage.creatorId).select {
                (UserMessage.creatorId.inList((if (members.isNotEmpty()) members else event.guild.members).map { it.user.id })) and
                        (UserMessage.channelId.inList((if (channels.isNotEmpty()) channels else event.guild.textChannels).map { it.id }))
            }
            val emotesByMember = result.map {
                it[UserMessage.creatorId] to emoteRegex.findAll(it[UserMessage.content], 0).toList()
            }.filterNot {
                (_, emotes) -> emotes.isEmpty()
            }.map { (creatorId, emotes) ->
                val emoteIds = emotes.map { it.value.dropLast(1).takeLast(18) }
                val member = event.jda.getUserById(creatorId)
                emoteIds.map { member to event.jda.getEmoteById(it) }
            }.flatten()
            emotesByMember
        }
                .groupingBy { it.second }
                .eachCount()
                .toList()
                .filter { it.first != null }
                .sortedByDescending { it.second }
                .take(100)

        val messageBuilder = MessageBuilder().append("[Top emotes in guild]\n")
        emotesUsed.forEachIndexed { i, (emote, count) ->
            messageBuilder.append("${i+1}. ").append(emote).append(": $count\n")
        }
        event.send(messageBuilder)
    }
}
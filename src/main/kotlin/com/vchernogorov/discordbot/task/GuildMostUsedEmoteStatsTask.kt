package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.UserMessage
import com.vchernogorov.discordbot.args.UserStatsArgs
import com.vchernogorov.discordbot.send
import mu.KLogger
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class GuildMostUsedEmoteStatsTask : AbstractCommandHandler() {
    override val logger: KLogger
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun handle(event: MessageReceivedEvent, args: UserStatsArgs) {
        val emotesUsed = transaction {
            val emoteRegex = "<:(.*?):[0-9]{18}>".toRegex()
            val result = UserMessage.slice(UserMessage.content, UserMessage.creatorId).select {
                (UserMessage.creatorId.inList((if (args.members.isNotEmpty()) args.members else event.guild.members).map { it.user.id })) and
                        (UserMessage.channelId.inList((if (args.channels.isNotEmpty()) args.channels else event.guild.textChannels).map { it.id }))
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
        }.filter {
            !it.first.isBot && it.second != null
        }
                .groupingBy { it.second }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .take(args.limitPrimaryResults)

        val messageBuilder = MessageBuilder().append("[Top emotes in guild]\n")
        emotesUsed.forEachIndexed { i, (emote, count) ->
            messageBuilder.append("${i+1}. ").append(emote).append(": $count\n")
        }
        event.send(messageBuilder)
    }
}
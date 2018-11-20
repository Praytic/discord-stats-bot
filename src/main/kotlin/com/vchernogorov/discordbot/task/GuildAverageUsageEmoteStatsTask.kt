package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.UserMessage
import com.vchernogorov.discordbot.UserStatsArgs
import com.vchernogorov.discordbot.send
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.time.temporal.ChronoUnit

class GuildAverageUsageEmoteStatsTask : MessagesStatsTask() {

    override fun execute(event: MessageReceivedEvent, args: UserStatsArgs) {
        val emotesUsed = transaction {
            val emoteRegex = "<:(.*?):[0-9]{18}>".toRegex()
            val result = UserMessage.slice(UserMessage.content, UserMessage.creatorId, UserMessage.creationDate).select {
                (UserMessage.creatorId.inList((if (args.members.isNotEmpty()) args.members else event.guild.members).map { it.user.id })) and
                        (UserMessage.channelId.inList((if (args.channels.isNotEmpty()) args.channels else event.guild.textChannels).map { it.id }))
            }
            val emotesByMember = result.map {
                Triple(it[UserMessage.creatorId], emoteRegex.findAll(it[UserMessage.content], 0).toList(), OffsetDateTime.parse(it[UserMessage.creationDate]))
            }.filterNot { (_, emotes, _) ->
                emotes.isEmpty()
            }.map { (creatorId, emotes, creationDate) ->
                val emoteIds = emotes.map { it.value.dropLast(1).takeLast(18) }
                val member = event.jda.getUserById(creatorId)
                emoteIds.map { Triple(member, event.jda.getEmoteById(it), creationDate) }
            }.flatten()
            emotesByMember
        }.filter { (_, emote, _) ->
            emote != null
        }

        val minCreationDateByEmote = emotesUsed.distinctBy { (_, emote, _) ->
            emote
        }.map { (_, emote, _) ->
            emote to emotesUsed.filter { it.second == emote }.map {  (_, _, creationDate) ->
                creationDate
            }.min()?.toLocalDate()
        }.toMap()

        val sortedEmotesUsed = emotesUsed.groupingBy {
            it.second
        }.eachCount().filter { (emote) ->
            emote != null
        }.map { (emote, count) ->
            val daysLive = ChronoUnit.DAYS.between(minCreationDateByEmote[emote], LocalDate.now())
            emote to count to count * 1.0 / daysLive
        }.toList().sortedByDescending { (_, usageRate) ->
            usageRate
        }

        val messageBuilder = MessageBuilder().append("[Emote average usage per day in guild]\n")
        sortedEmotesUsed.forEachIndexed { i, (emote, usageRate) ->
            if (args.tail && sortedEmotesUsed.count() - args.limitPrimaryResults <= i ||
                    !args.tail && args.limitPrimaryResults > i) {
                messageBuilder.append("${i + 1}. ").append(emote.first).append(": `%.2f | ${emote.second}`\n".format(usageRate))
            }
        }
        event.send(messageBuilder)
    }
}
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
import kotlin.math.roundToInt

class UserMostUsedEmoteStatsTask {

    fun execute(event: MessageReceivedEvent, member: Member, limitSecondaryResults: Int, channels: List<TextChannel>) {
        val emotesUsed = transaction {
            val emoteRegex = "<:(.*?):[0-9]{18}>".toRegex()
            val result = UserMessage.slice(UserMessage.content, UserMessage.creatorId).select {
                UserMessage.creatorId.eq(member.user.id) and
                        (UserMessage.channelId.inList((if (channels.isNotEmpty()) channels else event.guild.textChannels).map { it.id }))
            }

            result.map {
                emoteRegex.findAll(it[UserMessage.content], 0).toList()
            }.filterNot {
                it.isEmpty()
            }.map {
                it.map { event.jda.getEmoteById(it.value.dropLast(1).takeLast(18)) }
            }.flatten().filterNotNull()

        }
                .groupingBy { it }
                .eachCount()
                .toList()
        val topEmotesUsed = emotesUsed
                .sortedByDescending { it.second }
                .take(limitSecondaryResults)

        val messageBuilder = MessageBuilder().append("Top emotes for **${member.user.name}**: ")
        val totalEmotesUsed = emotesUsed.sumBy { it.second }
        topEmotesUsed.forEachIndexed { i, (emote, count) ->
            messageBuilder.append(emote).append(" `${(count * 1.0 / totalEmotesUsed * 100).roundToInt()}% | $count` ")
        }
        event.send(messageBuilder)
    }
}
package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.UserMessage
import com.vchernogorov.discordbot.UserStatsArgs
import com.vchernogorov.discordbot.send
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class UserEmoteStatsTask : MessagesStatsTask() {

    override fun execute(event: MessageReceivedEvent, args: UserStatsArgs) {
        val emotesByMember = transaction {
            val emoteRegex = "<:(.*?):[0-9]{18}>".toRegex()
            val result = UserMessage.slice(UserMessage.content, UserMessage.creatorId).select {
                (if (args.members.isNotEmpty()) UserMessage.creatorId.inList(args.members.map { it.user.id }) else UserMessage.creatorId.isNotNull()) and
                        (if (args.channels.isNotEmpty()) UserMessage.channelId.inList(args.channels.map { it.id }) else UserMessage.channelId.isNotNull())
            }
            val emotesByMember = result.map {
                it[UserMessage.creatorId] to emoteRegex.findAll(it[UserMessage.content], 0).toList()
            }.filterNot { (_, emotes) -> emotes.isEmpty() }
                    .map { (creatorId, emotes) ->
                val emoteIds = emotes.map { it.value.dropLast(1).takeLast(18) }
                val member = event.jda.getUserById(creatorId)
                member to emoteIds.map { event.jda.getEmoteById(it) }
            }
            emotesByMember
        }
                .groupBy({ it.first }, { it.second })
                .map { it.key to it.value.count() }
                .sortedByDescending { it.second }
                .take(args.limitSecondaryResults)

        var message = "```css\n" + "[Top emoters]\n"
        emotesByMember.forEachIndexed { i, it ->
            val user = it.first
            val emotesTotal = it.second
            message += "${i+1}. ${user.name}: $emotesTotal\n"
        }
        message += "```"
        event.send(message)
    }
}
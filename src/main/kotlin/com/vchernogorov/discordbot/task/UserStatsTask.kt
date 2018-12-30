package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.UserMessage
import com.vchernogorov.discordbot.args.UserStatsArgs
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class UserStatsTask : MessagesStatsTask() {
    override fun execute(event: MessageReceivedEvent, args: UserStatsArgs) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute(event: MessageReceivedEvent, vararg params: String) = with(event.guild) {
        //    val messages = loadAllMessages<UserMessage>(File(properties["messagesFolder"] as String))
        val textChannelIds = if (params.isEmpty()) textChannels.map { it.id } else params
                .map { name -> getTextChannelsByName(name, true) }
                .flatten()
                .map { it.id }
                .toSet()
        val memberIds = (if (params.isEmpty()) members.map { it.user.id to it.user.name } else params
                .map { name ->
                    listOf<Member>()
                            .union(getMembersByName(name, true))
                            .union(getMembersByNickname(name, true))
                            .union(getMembersByEffectiveName(name, true))
                }
                .flatten()
                .map { it.user.id to it.user.name }).toMap()

        transaction {
            UserMessage.select {
                if (textChannelIds.isEmpty()) {
                    UserMessage.creatorId.inList(memberIds.keys)
                } else if (memberIds.isEmpty()) {
                    UserMessage.channelId.inList(textChannelIds)
                } else {
                    UserMessage.channelId.inList(textChannelIds) and
                            UserMessage.creatorId.inList(memberIds.keys)
                }
            }.map { rs ->
                with(UserMessage) {
                    val id = rs[id]
                    val channelId = rs[channelId]
                    val content = rs[content]
                    val creatorId = rs[creatorId]
                    val creationDate = rs[creationDate]
                }
            }
        }
        Unit
    }
}
package com.vchernogorov.discordbot

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

/**
 * Queries manager contains different [Query]s for common use in the application.
 * [chunkSize] is used to limit big requests by selecting no more than [chunkSize] records in one go.
 */
class QueriesManager(val chunkSize: Int) {

    val chunksEnabled = chunkSize > 0 && chunkSize != Int.MAX_VALUE

    /**
     * Selects [UserMessage]s sliced by [UserMessage.content], [UserMessage.creatorId], [UserMessage.creationDate]
     * where [UserMessage.creatorId] is in [members] or in [Guild.getMembers] and [UserMessage.channelId] is in
     * [channels] or in [Guild.getTextChannels].
     */
    fun selectUserMessagesByMembersAndChannels(guild: Guild, members: List<Member>, channels: List<TextChannel>): Query {
        return UserMessage
                .slice(
                        UserMessage.content,
                        UserMessage.creatorId,
                        UserMessage.creationDate
                )
                .select {
                    (UserMessage.creatorId.inList((if (members.isNotEmpty()) members else guild.members).map { it.user.id })) and
                            (UserMessage.channelId.inList((if (channels.isNotEmpty()) channels else guild.textChannels).map { it.id }))
                }
    }

    /**
     * Selects [UserMessage]s sliced by [UserMessage.id], [UserMessage.channelId], [UserMessage.creationDate]
     * where [UserMessage.channelId] is in [channels].
     */
    fun selectUserMessagesByChannels(channels: List<TextChannel>) =
            UserMessage
                    .slice(
                            UserMessage.id,
                            UserMessage.channelId,
                            UserMessage.creationDate
                    )
                    .select {
                        UserMessage.channelId.inList(channels.map { it.id })
                    }

    fun selectIds(offset: Int) =
            TempId.slice(TempId.id).selectAll().orderBy(TempId.id).limit(chunkSize, offset)
}
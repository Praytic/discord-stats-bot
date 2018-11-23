package com.vchernogorov.discordbot

import org.jetbrains.exposed.sql.Query
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

/**
 * Transaction manager contains different transactions for common use in the application.
 * [queriesManager] is used for getting common [Query]s.
 */
class TransactionsManager(val queriesManager: QueriesManager) {

    /**
     * Returns [Triple] of [User.getId], [Emote.getId] and [Message.getCreationTime]
     * Results are filtered by [UserStatsArgs.members] and [UserStatsArgs.channels] if they are not empty.
     * Otherwise results are filtered by [Guild.getMembers] and [Guild.getTextChannels].
     */
    fun selectEmotesByCreatorsAndCreationDate(guild: Guild, args: UserStatsArgs) = transaction {
        val emoteRegex = "<:(.*?):[0-9]{18}>".toRegex()
        queriesManager.selectUserMessagesByMembersAndChannels(guild, args.members, args.channels).map {
            Triple(
                    it[UserMessage.creatorId],
                    emoteRegex.findAll(it[UserMessage.content], 0).toList(),
                    OffsetDateTime.parse(it[UserMessage.creationDate])
            )
        }.filter { (creatorId, emotes, _) ->
            emotes.isNotEmpty() && creatorId != null
        }.map { (creatorId, emotes, creationDate) ->
            val emoteIds = emotes.map { it.value.dropLast(1).takeLast(18) }
            emoteIds.map { Triple(creatorId!!, it, creationDate) }
        }.flatten()
    }

    /**
     * Returns [Map] of [UserMessage.id] values mapped by [UserMessage.channelId] keys.
     * [UserMessage.id] is an ID of the latest [UserMessage] saved in the database by [UserMessage.creationDate].
     * [UserMessage.channelId]s should be contained in [channelIds] list.
     */
    fun latestSavedMessages(channels: List<TextChannel>) = transaction {
        queriesManager.selectUserMessagesByChannels(channels).groupBy {
            it[UserMessage.channelId]
        }.map {
            it.key to it.value.maxBy {
                OffsetDateTime.parse(it[UserMessage.creationDate])
            }?.get(UserMessage.id)
        }.toMap()
    }

    /**
     * Returns [Map] of [UserMessage.id] values mapped by [UserMessage.channelId] keys.
     * [UserMessage.id] is an ID of the earliest [UserMessage] saved in the database by [UserMessage.creationDate].
     * [UserMessage.channelId]s should be contained in [channelIds] list.
     */
    fun firstSavedMessages(channels: List<TextChannel>) = transaction {
        queriesManager.selectUserMessagesByChannels(channels).groupBy {
            it[UserMessage.channelId]
        }.map {
            it.key to it.value.minBy {
                OffsetDateTime.parse(it[UserMessage.creationDate])
            }?.get(UserMessage.id)
        }.toMap()
    }

    /**
     * Uploads a collections of [Message]s into [UserMessage] table.
     */
    fun uploadMessages(elements: Collection<Message>, ignoreExistingRecords: Boolean = true) = transaction {
        UserMessage.batchInsert(elements, ignore = ignoreExistingRecords) {
            this[UserMessage.id] = it.id
            this[UserMessage.channelId] = it.channel.id
            this[UserMessage.creationDate] = it.creationTime.toString()
            this[UserMessage.creatorId] = it.author.id
            this[UserMessage.content] = it.contentRaw
        }
    }

}
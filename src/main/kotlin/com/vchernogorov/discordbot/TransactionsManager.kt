package com.vchernogorov.discordbot

import mu.KotlinLogging
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.sql.ResultSet

/**
 * Transaction manager contains different transactions for common use in the application.
 * [queriesManager] is used for getting common [Query]s.
 */
class TransactionsManager(val queriesManager: QueriesManager) {

    private val logger = KotlinLogging.logger {}

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
                    it[UserMessage.creationDate]
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
        channels.map {
            val messageId = queriesManager.findMessageIdByMaxCreationDate(it).singleOrNull()?.get(UserMessage.id)
            it.id to messageId
        }.toMap()
    }

    /**
     * Returns [Map] of [UserMessage.id] values mapped by [UserMessage.channelId] keys.
     * [UserMessage.id] is an ID of the earliest [UserMessage] saved in the database by [UserMessage.creationDate].
     * [UserMessage.channelId]s should be contained in [channelIds] list.
     */
    fun firstSavedMessages(channels: List<TextChannel>) = transaction {
        channels.map {
            val messageId = queriesManager.findMessageIdByMinCreationDate(it).singleOrNull()?.get(UserMessage.id)
            it.id to messageId
        }.toMap()
    }

//    fun getSavedMessages(channels: List<TextChannel>, filter: (List<ResultRow>) -> ResultRow?) = transaction {
//        val map = mutableMapOf<String, String?>()
//        if (queriesManager.chunksEnabled) {
//            this@TransactionsManager.logger.debug { "Messages will be fetched by chunks." }
//            selectByChunks(queriesManager.selectUserMessagesByChannels(channels)).forEach {
//                val toMap = it.groupBy {
//                    it[UserMessage.channelId]
//                }.map { (channelId, resultRows) ->
//                    channelId to filter(resultRows)?.get(UserMessage.id)
//                }.toMap()
//                map.putAll(toMap)
//            }
//        } else {
//            this@TransactionsManager.logger.debug { "Chunked fetching is disabled." }
//            queriesManager.selectUserMessagesByChannels(channels).groupBy {
//                it[UserMessage.channelId]
//            }.map { (channelId, resultRows) ->
//                channelId to filter(resultRows)?.get(UserMessage.id)
//            }.forEach { (channelId, maxResultRow) ->
//                map[channelId] = maxResultRow
//            }
//        }
//        map
//    }

    /**
     * Uploads a collections of [Message]s into [UserMessage] table.
     */
    fun uploadMessages(elements: Collection<Message>, ignoreExistingRecords: Boolean = true) = transaction {
        UserMessage.batchInsert(elements, ignore = ignoreExistingRecords) {
            this[UserMessage.id] = it.id
            this[UserMessage.channelId] = it.channel.id
            this[UserMessage.creationDate] = DateTime(it.creationTime.toInstant().toEpochMilli())
            this[UserMessage.creatorId] = it.author.id
            this[UserMessage.content] = it.contentRaw
        }
    }

    fun selectByChunks(query: Query) = transaction {
        kotlin.sequences.sequence {
            val prepareSQL = query.prepareSQL(QueryBuilder(false))
            val createTempTable = "CREATE TEMPORARY TABLE ${TempId.nameInDatabaseCase()} AS (" +
                    "  SELECT prep.${TempId.id.name}" +
                    "  FROM ($prepareSQL) AS prep" +
                    "  ORDER BY prep.${TempId.id.name}" +
                    ")"
            exec(createTempTable)
            var rowsExtracted: Int? = null
            var offset = 0
            val queryRess = mutableListOf<List<ResultRow>>()
            while (rowsExtracted != 0) {
                val messageIds = queriesManager.selectIds(offset).map { it[TempId.id] }
                val queryRes = query.adjustWhere {
                    Op.build {
                        UserMessage.id.inList(messageIds)
                    }
                }.toList()
                rowsExtracted = queryRes.count()
                offset += queryRes.count()
                yield(queryRes)
            }
            exec("DROP TEMPORARY TABLE IF EXISTS ${TempId.nameInDatabaseCase()}")
        }
    }

    fun <T : Any> String.execAndMap(transform: (ResultSet) -> T): List<T> {
        val result = arrayListOf<T>()
        TransactionManager.current().exec(this) { rs ->
            while (rs.next()) {
                result += transform(rs)
            }
        }
        return result
    }
}
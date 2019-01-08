package com.vchernogorov.discordbot.manager

import com.google.gson.Gson
import com.vchernogorov.discordbot.TempId
import com.vchernogorov.discordbot.UserMessage
import com.vchernogorov.discordbot.MemberStat
import com.vchernogorov.discordbot.args.GuildStatsArgs
import com.vchernogorov.discordbot.args.MemberStatsArgs
import com.vchernogorov.discordbot.args.StringOccurrenceArgs
import com.vchernogorov.discordbot.cache.CacheManager
import com.vchernogorov.discordbot.mapper.TopEmoteDailyUsageStatsMapper
import com.vchernogorov.discordbot.mapper.MemberStatsMapper
import com.vchernogorov.discordbot.mapper.StringOccurrenceMapper
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
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
class TransactionsManager(val queriesManager: QueriesManager,
                          val gson: Gson) {

    lateinit var cacheManager: CacheManager

    /**
     * Returns [Triple] of [User.getId], [Emote.getId] and [Message.getCreationTime] for every emote in guild.
     * Returned results may be filtered by a list of members or channels.
     */
    fun countStringOccurrencesByMember(guild: Guild, args: StringOccurrenceArgs): Map<String, Long> = transaction {
        val cachedResult = if (::cacheManager.isInitialized) {
            cacheManager.getFromCache(guild, args, StringOccurrenceMapper(gson, guild, args.strings)::map)
        } else null
        if (cachedResult != null) return@transaction cachedResult

        val resultRows = queriesManager.selectUserMessagesByStringOccurrence(args.strings, guild, args.members, args.channels)

        if (resultRows.toList().isEmpty()) {
            throw Exception("No messages found for guild $guild with arguments $args.")
        }
        val emotesList = if (queriesManager.chunksEnabled) {
            val resultMap = mutableMapOf<String, Long>()
            selectByChunks(resultRows, StringOccurrenceMapper(gson, guild, args.strings)::map).forEach {
                resultMap.putAll(it)
            }
            resultMap
        }
        else {
            StringOccurrenceMapper(gson, guild, args.strings).map(resultRows.toList())
        }

        if (::cacheManager.isInitialized) {
            cacheManager.saveToCache(guild, args, StringOccurrenceMapper(gson, guild, args.strings).map(emotesList))
        }
        emotesList
    }

    /**
     * Returns [Triple] of [User.getId], [Emote.getId] and [Message.getCreationTime] for every emote in guild.
     * Returned results may be filtered by a list of members or channels.
     */
    fun selectEmotesByCreatorsAndCreationDate(guild: Guild, args: GuildStatsArgs): List<Triple<String, String, DateTime>> = transaction {
        val cachedResult = if (::cacheManager.isInitialized) {
            cacheManager.getFromCache(guild, args, TopEmoteDailyUsageStatsMapper(gson, guild)::map)
        } else null
        if (cachedResult != null) return@transaction cachedResult

        val resultRows = queriesManager.selectUserMessagesByMembersAndChannels(guild, args.members, args.channels)

        if (resultRows.toList().isEmpty()) {
            throw Exception("No emotes found for guild $guild with arguments $args.")
        }
        val emotesList = if (queriesManager.chunksEnabled) {
            selectByChunks(resultRows, TopEmoteDailyUsageStatsMapper(gson, guild)::map).flatten()
        }
        else {
            TopEmoteDailyUsageStatsMapper(gson, guild).map(resultRows.toList())
        }

        if (::cacheManager.isInitialized) {
            cacheManager.saveToCache(guild, args, TopEmoteDailyUsageStatsMapper(gson, guild).map(emotesList))
        }
        emotesList
    }

    /**
     * Returns [MemberStat] for specified [Member].
     */
    fun selectMemberStat(member: Member, args: MemberStatsArgs): MemberStat = transaction {
        val cachedResult = if (::cacheManager.isInitialized) {
            cacheManager.getFromCache(member, args, MemberStatsMapper(gson, member)::map)
        } else null
        if (cachedResult != null) return@transaction cachedResult

        val query = queriesManager.selectUserMessagesByMemberAndChannels(member, args.channels)
        val filteredByYears = if (args.years.isNotEmpty()) {
            query.filter {
                args.years.contains(it[UserMessage.creationDate].year().get())
            }
        } else {
            query.toList()
        }
        val filteredByMonths = if (args.months.isNotEmpty() && args.years.isNotEmpty()) {
            filteredByYears.filter {
                args.months.contains(it[UserMessage.creationDate].monthOfYear().get())
            }
        } else {
            filteredByYears
        }

        if (filteredByMonths.isEmpty()) {
            throw Exception("No messages found for member $member with arguments $args.")
        }
        val memberStat = MemberStatsMapper(gson, member).map(filteredByMonths)

        if (::cacheManager.isInitialized) {
            cacheManager.saveToCache(member, args, MemberStatsMapper(gson, member).map(memberStat))
        }
        memberStat
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

    fun <T> selectByChunks(query: Query, processor: (List<ResultRow>) -> T): List<T> = transaction {
        val prepareSQL = query.prepareSQL(QueryBuilder(false))
        val processedResult = mutableListOf<T>()
        try {
            val createTempTable = "CREATE TEMPORARY TABLE ${TempId.nameInDatabaseCase()} AS (" +
                    "  SELECT prep.${TempId.id.name}" +
                    "  FROM ($prepareSQL) AS prep" +
                    "  ORDER BY prep.${TempId.id.name}" +
                    ")"
            exec(createTempTable)
            var rowsExtracted: Int? = null
            var offset = 0
            while (rowsExtracted != 0) {
                val messageIds = queriesManager.selectIds(offset).map { it[TempId.id] }
                val queryRes = query.adjustWhere {
                    Op.build {
                        UserMessage.id.inList(messageIds)
                    }
                }.toList()
                rowsExtracted = queryRes.count()
                offset += queryRes.count()
                processedResult.add(processor(queryRes))
            }
        } finally {
            exec("DROP TEMPORARY TABLE IF EXISTS ${TempId.nameInDatabaseCase()}")
        }
        processedResult
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
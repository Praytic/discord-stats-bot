package com.vchernogorov.discordbot.cache

import com.google.gson.Gson
import com.vchernogorov.discordbot.Mode
import com.vchernogorov.discordbot.manager.TransactionsManager
import com.vchernogorov.discordbot.args.UserStatsArgs
import com.vchernogorov.discordbot.mapper.TopEmoteDailyUsageStatsMapper
import com.xenomachina.argparser.ArgParser
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import org.joda.time.format.DateTimeFormat
import kotlin.concurrent.fixedRateTimer
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.reflect

/**
 * Schedules cache filling via [cacheManager] for common handlers.
 */
class CacheScheduler(val cacheManager: CacheManager,
                     val transactionsManager: TransactionsManager,
                     val jda: JDA,
                     val gson: Gson,
                     val cacheSchedulerPeriod: Long) {

    /**
     * Starts scheduler for all time-consuming transactions.
     */
    fun start() {
        fixedRateTimer("Cache scheduler", true, 1000, cacheSchedulerPeriod) {
            cacheCommon(
                    Mode.TOP_EMOTE_DAILY_USAGE_STATS,
                    transactionsManager::selectEmotesByCreatorsAndCreationDate) { result, guild ->
                TopEmoteDailyUsageStatsMapper(gson, guild).map(result)
            }
        }
    }

    /**
     * General method for caching result of [transactionFunction] which is converted by [mapper] to [String].
     * [command] parameter is needed to create default [UserStatsArgs] for the transaction.
     */
    fun <T> cacheCommon(
            command: Mode,
            transactionFunction: (guild: Guild, userStatsArgs: UserStatsArgs) -> T,
            mapper: (result: T, guild: Guild) -> String
    ): Map<Guild, String> {
        val cacheKeys = mutableMapOf<Guild, String>()
        jda.guilds.forEach { guild ->
            val args = ArgParser(arrayOf()).parseInto { UserStatsArgs(it, guild, command) }
            val result = transactionFunction.invoke(guild, args)
            val cacheKey = cacheManager.saveToCache(guild, args, mapper.invoke(result, guild))
            cacheKeys.put(guild, cacheKey)
        }
        return cacheKeys
    }
}
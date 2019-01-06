package com.vchernogorov.discordbot.cache

import com.google.gson.Gson
import com.vchernogorov.discordbot.Mode
import com.vchernogorov.discordbot.manager.TransactionsManager
import com.vchernogorov.discordbot.args.GuildStatsArgs
import com.vchernogorov.discordbot.mapper.TopEmoteDailyUsageStatsMapper
import com.xenomachina.argparser.ArgParser
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import kotlin.concurrent.fixedRateTimer

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
                    Mode.GUILD_AVG_EMOTE_USAGE,
                    transactionsManager::selectEmotesByCreatorsAndCreationDate) { result, guild ->
                TopEmoteDailyUsageStatsMapper(gson, guild).map(result)
            }
        }
    }

    /**
     * General method for caching result of [transactionFunction] which is converted by [mapper] to [String].
     * [command] parameter is needed to create default [GuildStatsArgs] for the transaction.
     */
    fun <T> cacheCommon(
            command: Mode,
            transactionFunction: (guild: Guild, guildStatsArgs: GuildStatsArgs) -> T,
            mapper: (result: T, guild: Guild) -> String
    ): Map<Guild, String> {
        val cacheKeys = mutableMapOf<Guild, String>()
        jda.guilds.forEach { guild ->
            val args = ArgParser(arrayOf()).parseInto { GuildStatsArgs(it, guild, command) }
            val result = transactionFunction.invoke(guild, args)
            val cacheKey = cacheManager.saveToCache(guild, args, mapper.invoke(result, guild))
            cacheKeys.put(guild, cacheKey)
        }
        return cacheKeys
    }
}
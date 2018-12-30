package com.vchernogorov.discordbot.cache

import com.google.gson.Gson
import com.vchernogorov.discordbot.Mode
import com.vchernogorov.discordbot.manager.TransactionsManager
import com.vchernogorov.discordbot.args.UserStatsArgs
import com.xenomachina.argparser.ArgParser
import net.dv8tion.jda.core.JDA
import org.joda.time.format.DateTimeFormat
import kotlin.concurrent.fixedRateTimer

/**
 * Schedules cache filling via [cacheManager] for common tasks.
 */
class CacheScheduler(val cacheManager: CacheManager,
                     val transactionsManager: TransactionsManager,
                     val jda: JDA,
                     val gson: Gson,
                     val cacheSchedulerPeriod: Long) {

    private val dateformatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    /**
     * Starts scheduler for all time-consuming transactions.
     */
    fun start() {
        fixedRateTimer("Cache scheduler", true, 1000, cacheSchedulerPeriod) {
            startForEmotesByCreatorsAndCreationDate()
        }
    }

    /**
     * Caches [LIMIT_PRIMARY_RESULTS_MAX] results of [TransactionsManager.selectEmotesByCreatorsAndCreationDate] method.
     */
    fun startForEmotesByCreatorsAndCreationDate() {
        jda.guilds.forEach { guild ->
            val params = arrayOf("${Mode.TOP_EMOTE_DAILY_USAGE_STATS} " +
                    "--limitPrimaryResults=${UserStatsArgs.LIMIT_PRIMARY_RESULTS_MAX} " +
                    "--limitSecondaryResults${UserStatsArgs.LIMIT_SECONDARY_RESULTS_MAX}")
            val args = ArgParser(params).parseInto { UserStatsArgs(it, guild) }
            val result = transactionsManager.selectEmotesByCreatorsAndCreationDate(guild, args)
            cacheManager.saveToCache("selectEmotesByCreatorsAndCreationDate", guild, args, result) {
                val value = it.map {
                    Triple(it.first, it.second, it.third.toString(dateformatter))
                }
                gson.toJson(value)
            }
        }
    }
}
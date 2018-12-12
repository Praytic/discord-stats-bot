package com.vchernogorov.discordbot

import com.google.gson.Gson
import com.xenomachina.argparser.ArgParser
import net.dv8tion.jda.core.JDA
import org.joda.time.format.DateTimeFormat
import kotlin.concurrent.fixedRateTimer

class CacheScheduler(val cacheManager: CacheManager,
                     val transactionsManager: TransactionsManager,
                     val jda: JDA,
                     val gson: Gson) {

    private val dateformatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    fun start() {
        fixedRateTimer("Cache scheduler", true, 1000, 60*60*24*1000) {
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
}
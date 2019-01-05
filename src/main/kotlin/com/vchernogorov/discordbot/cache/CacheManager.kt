package com.vchernogorov.discordbot.cache

import com.vchernogorov.discordbot.args.UserStatsArgs
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import redis.clients.jedis.JedisPool
import java.util.*

/**
 * Cache manager consists of common methods for working with [JedisPool].
 */
class CacheManager(val jedisPool: JedisPool, val cacheExpiration: Int) {

    /**
     * Creates a key from [guild] and [args]. Grabs the value by created key from [JedisPool]
     * and converts it to [T] type using provided [mapper].
     * Returns the value associated with the key or null if no value found by the generated key.
     */
    fun <T> getFromCache(guild: Guild, args: UserStatsArgs, mapper: (String) -> T): T? {
        return jedisPool.resource.use {
            val key = "${args.command}/${guild.id}"
            val value = it.get(key)
            if (value == null) null else mapper.invoke(value)
        }
    }

    /**
     * Creates a key from [member] and [args]. Grabs the value by created key from [JedisPool]
     * and converts it to [T] type using provided [mapper].
     * Returns the value associated with the key or null if no value found by the generated key.
     */
    fun <T> getFromCache(member: Member, args: UserStatsArgs, mapper: (String) -> T): T? {
        return jedisPool.resource.use {
            val key = "${args.command}/${member.user.id}"
            val value = it.get(key)
            if (value == null) null else  mapper.invoke(value)
        }
    }

    /**
     * Creates a key from [member] and [args]. Saves converted by [mapper] value to [JedisPool] using created key.
     * Expiration time of the cached value is set by [MyArgs.cacheExpiration] parameter.
     * Returns generated key.
     */
    fun saveToCache(member: Member, args: UserStatsArgs, value: String): String {
        return jedisPool.resource.use {
            val key = "${args.command}/${member.guild.id}/${member.user.id}"
            it.set(key, value)
            it.expire(key, cacheExpiration)
            key
        }
    }

    /**
     * Creates a key from [guild] and [args]. Saves converted by [mapper] value to [JedisPool] using created key.
     * Expiration time of the cached value is set by [MyArgs.cacheExpiration] parameter.
     * Returns generated key.
     */
    fun saveToCache(guild: Guild, args: UserStatsArgs, value: String): String {
        return jedisPool.resource.use {
            val key = "${args.command}/${guild.id}"
            it.set(key, value)
            it.expire(key, cacheExpiration)
            key
        }
    }
}
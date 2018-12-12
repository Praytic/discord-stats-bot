package com.vchernogorov.discordbot

import net.dv8tion.jda.core.entities.Guild
import redis.clients.jedis.JedisPool
import java.util.*

/**
 * Cache manager consists of common methods for working with [JedisPool].
 */
class CacheManager(val jedisPool: JedisPool) {

    /**
     * Creates a key from [primaryKey], [guild], [args] and grabs the value (if exists) from [JedisPool]
     * and converts it to [T] type using provided [mapper].
     * Returns null if no value associated with the generated key.
     */
    fun <T> getFromCache(primaryKey: String, guild: Guild, args: UserStatsArgs, mapper: (String) -> T): T? {
        return jedisPool.resource.use {
            val membersHash = Math.abs(Arrays.hashCode(
                    ((if (args.members.isNotEmpty()) args.members else guild.members).map { it.user.id }).toTypedArray()
            ))
            val channelsHash = Math.abs(Arrays.hashCode(
                    ((if (args.channels.isNotEmpty()) args.channels else guild.channels).map { it.id }).toTypedArray()
            ))
            val key = "$primaryKey/${guild.id}/$channelsHash/$membersHash"
            val value = it.get(key)
            if (value == null) null else mapper.invoke(value)
        }
    }

    /**
     * Creates a key from [primaryKey], [guild], [args] and saves converted by [mapper] value to [JedisPool].
     * Expiration time of the cached value is set by [MyArgs.cacheExpiration] parameter.
     */
    fun <T> saveToCache(primaryKey: String, guild: Guild, args: UserStatsArgs, value: T, mapper: (T) -> String): String {
        return jedisPool.resource.use {
            val membersHash = Math.abs(Arrays.hashCode(
                    ((if (args.members.isNotEmpty()) args.members else guild.members).map { it.user.id }).toTypedArray()
            ))
            val channelsHash = Math.abs(Arrays.hashCode(
                    ((if (args.channels.isNotEmpty()) args.channels else guild.channels).map { it.id }).toTypedArray()
            ))
            val key = "$primaryKey/${guild.id}/$channelsHash/$membersHash"
            it.set(key, mapper(value))
            it.expire(key, 60*60*24)
            key
        }
    }
}
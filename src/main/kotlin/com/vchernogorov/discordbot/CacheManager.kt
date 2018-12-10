package com.vchernogorov.discordbot

import net.dv8tion.jda.core.entities.Guild
import redis.clients.jedis.JedisPool
import java.util.*

class CacheManager(val jedisPool: JedisPool) {

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
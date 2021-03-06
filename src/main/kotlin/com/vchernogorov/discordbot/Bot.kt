package com.vchernogorov.discordbot

import com.google.gson.Gson
import com.vchernogorov.discordbot.args.ApplicationArgs
import com.vchernogorov.discordbot.cache.CacheManager
import com.vchernogorov.discordbot.cache.CacheScheduler
import com.vchernogorov.discordbot.listener.FetchMessagesListener
import com.vchernogorov.discordbot.listener.MainCommandListener
import com.vchernogorov.discordbot.manager.QueriesManager
import com.vchernogorov.discordbot.manager.TransactionsManager
import com.vchernogorov.discordbot.task.GenericCommandHandler
import com.xenomachina.argparser.ArgParser
import mu.KLogger
import mu.KotlinLogging
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ratpack.health.HealthCheckHandler
import ratpack.server.BaseDir
import ratpack.server.RatpackServer.start
import redis.clients.jedis.JedisPool
import java.net.URI

fun main(args: Array<String>) = ArgParser(args).parseInto(::ApplicationArgs).run {
    val gson = Gson()
    val logger = KotlinLogging.logger { }
    printArgs(logger)
    val server = start {
        it.serverConfig {
            it.baseDir(BaseDir.find()).env()
        }.handlers { it.get("health", HealthCheckHandler()) }
    }

    val jedisPool = if (System.getenv("REDIS_URL") != null) {
        JedisPool(System.getenv("REDIS_URL"))
    } else {
        JedisPool()
    }

    try {
        val queriesManager = QueriesManager(chunkSize)
        val transactionsManager = TransactionsManager(queriesManager, gson)
        val commandHandler = GenericCommandHandler(transactionsManager)
        val ownerCommandListener = MainCommandListener(authorizedUsers, printErrorsToDiscord, removeOriginalRequest, commandHandler)
        val listeners = mutableListOf<ListenerAdapter>(ownerCommandListener)
        if (fetchMessages) {
            listeners.add(FetchMessagesListener(fetchDelay, backoffRetryDelay, backoffRetryFactor, transactionsManager))
        }
        val jda = initJda(listeners)
        if (!disableCache) {
            val cacheManager = CacheManager(jedisPool, cacheExpiration)
            transactionsManager.cacheManager = cacheManager
            val cacheScheduler = CacheScheduler(cacheManager, transactionsManager, jda, gson, cacheSchedulerPeriod)
            cacheScheduler.start()
        }

        initDatabase(createSchemas, logger)
    } catch (e: Throwable) {
        logger.error(e) { "Stopping app because of the initialization error." }
        server.stop()
        jedisPool.close()
        System.exit(-1)
    }
    Unit
}

fun initDatabase(createSchemas: Boolean, logger: KLogger): Database {
    val dbUri = URI(System.getenv("CLEARDB_DATABASE_URL"))
    val username = dbUri.userInfo.split(":")[0]
    val password = dbUri.userInfo.split(":")[1]
    val dbUrl = "jdbc:mysql://" + dbUri.host + dbUri.path
    val connect = Database.connect(
            url = dbUrl,
            driver = "com.mysql.jdbc.Driver",
            user = username,
            password = password,
            setupConnection = {
                it.autoCommit = false
            })
    logger.info("Database connection has been established to $dbUrl for user $username.")
    if (createSchemas) {
        transaction {
            logger.info("Create missing schemas.")
            SchemaUtils.createMissingTablesAndColumns(UserMessage)
        }
    }
    return connect
}

fun initJda(listeners: List<ListenerAdapter>): JDA {
    val jdaBuilder = JDABuilder(AccountType.BOT)
    listeners.forEach {
        jdaBuilder.addEventListener(it)
    }
    return jdaBuilder
            .setToken(System.getenv("BOT_TOKEN") ?: throw Exception("Token wasn't populated."))
            .build()
}
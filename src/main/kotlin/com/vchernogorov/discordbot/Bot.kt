package com.vchernogorov.discordbot

import com.google.gson.Gson
import com.xenomachina.argparser.ArgParser
import mu.KLogger
import mu.KotlinLogging
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ratpack.health.HealthCheckHandler
import ratpack.server.BaseDir
import ratpack.server.RatpackServer.start
import java.net.URI

fun main(args: Array<String>) = ArgParser(args).parseInto(::MyArgs).run {
    val gson = Gson()
    val logger = KotlinLogging.logger { }
    val server = start {
        it.serverConfig {
            it.baseDir(BaseDir.find()).env()
        }.handlers { it.get("health", HealthCheckHandler()) }
    }

    try {
        initDatabase(createSchemas, logger)
        initJda(BotInitializerListener(fetchDelay, backoffRetryDelay, backoffRetryFactor, hugeTransactions))
    } catch (e: Throwable) {
        logger.error(e) { "Stopping app because of the initialization error." }
        server.stop()
        System.exit(-1)
    }
    Unit
}

fun initDatabase(createSchemas: Boolean, logger: KLogger): Database {
    val dbUri = URI(System.getenv("CLEARDB_DATABASE_URL"))
    val username = dbUri.userInfo.split(":")[0]
    val password = dbUri.userInfo.split(":")[1]
    val dbUrl = "jdbc:mysql://" + dbUri.host + dbUri.path
    val connect = Database.connect(url = dbUrl, driver = "com.mysql.jdbc.Driver", user = username, password = password)
    logger.info("Database connection has been established to $dbUrl for user $username.")
    if (createSchemas) {
        transaction {
            logger.info("Create missing schemas.")
            SchemaUtils.createMissingTablesAndColumns(UserMessage)
        }
    }
    return connect
}

fun initJda(botInitializer: BotInitializerListener): JDA {
    return JDABuilder(AccountType.BOT)
            .addEventListener(OwnerCommandListener())
            .addEventListener(botInitializer)
            .setToken(System.getenv("BOT_TOKEN") ?: throw Exception("Token wasn't populated."))
            .build()
}
package com.vchernogorov.discordbot

import com.google.gson.Gson
import com.xenomachina.argparser.ArgParser
import kotlinx.coroutines.*
import mu.KotlinLogging
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.TextChannel
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
  val db: Database
  val jda: JDA

  try {
    db = {
      val dbUri = URI(System.getenv("CLEARDB_DATABASE_URL"))
      val username = dbUri.getUserInfo().split(":")[0]
      val password = dbUri.getUserInfo().split(":")[1]
      val dbUrl = "jdbc:mysql://" + dbUri.getHost() + dbUri.getPath()
      val connect = Database.connect(url = dbUrl, driver = "com.mysql.jdbc.Driver", user = username, password = password)
      logger.info("Database connection has been established to $dbUrl for user $username.")
      if (createSchemas) {
        createMissingSchemas()
      }
      connect
    }.invoke()
    jda = JDABuilder(AccountType.BOT)
        .addEventListener(OwnerCommandListener())
        .addEventListener(BotInitializerListener(fetchDelay, backoffRetryDelay))
        .setToken(System.getenv("BOT_TOKEN") ?: throw Exception("Token wasn't populated."))
        .build()
  } catch (e: Throwable) {
    e.printStackTrace()
    server.stop()
    System.exit(-1)
  }

  Unit
}

suspend fun uploadNewMessages(channel: TextChannel, messagesByChannel: MutableMap<TextChannel, String?>) = coroutineScope {
  var latestSavedMessageId = messagesByChannel[channel]
  val latestMessageId = channel.getLatestMessageIdSafe()
  async {
    var newMessagesUploaded = 0
    while (true) {
      val newMessages = channel.getHistoryAfter(
          if (latestMessageId == null || latestMessageId == latestSavedMessageId) {
            break
          } else if (latestSavedMessageId == null) {
            latestMessageId
          } else {
            latestSavedMessageId
          }, 100).complete().retrievedHistory
      if (newMessages.isNotEmpty()) {
        uploadMessages(newMessages)
        latestSavedMessageId = newMessages.maxBy { it.creationTime }?.id
        newMessagesUploaded += newMessages.size
      }
    }
    messagesByChannel[channel] = latestSavedMessageId
    newMessagesUploaded
  }
}

suspend fun uploadOldMessages(channel: TextChannel, messagesByChannel: MutableMap<TextChannel, String?>) = coroutineScope {
  var firstSavedMessageId = messagesByChannel[channel] ?: channel.getLatestMessageIdSafe()
  async {
    var newMessagesUploaded = 0
    while (true) {
      val newMessages = channel.getHistoryBefore(
          if (firstSavedMessageId == null) {
            break
          } else {
            firstSavedMessageId
          }, 100).complete().retrievedHistory
      if (newMessages.isEmpty()) {
        break
      }
      if (newMessages.isNotEmpty()) {
        uploadMessages(newMessages)
        firstSavedMessageId = newMessages.minBy { it.creationTime }?.id
        newMessagesUploaded += newMessages.size
      }
    }
    messagesByChannel[channel] = firstSavedMessageId
    newMessagesUploaded
  }
}

fun createMissingSchemas() {
  transaction {
    println("Create missing schemas.")
    SchemaUtils.createMissingTablesAndColumns(UserMessage)
  }
}
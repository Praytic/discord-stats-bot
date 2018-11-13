package com.vchernogorov.discordbot

import com.google.gson.Gson
import kotlinx.coroutines.*
import mu.KotlinLogging
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.utils.PermissionUtil
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ratpack.health.HealthCheckHandler
import ratpack.server.BaseDir
import ratpack.server.RatpackServer.start
import java.net.URI
import java.time.Instant.now

val lastMessageByChannel by lazy {
  logger.info("Initializing 'last messages IDs by channel' map.")
  val map = mutableMapOf<TextChannel, String?>()
  for (guild in jda.guilds) {
    // Not working because of GC overhead error
//    val messageIds = latestSavedMessages(guild.textChannels.map { it.id })
    for (channel in guild.textChannels) {
      val messageIds = latestSavedMessages(listOf(channel.id))
      map[channel] = messageIds[channel.id]
    }
  }
  map
}

val firstMessageByChannel by lazy {
  logger.info("Initializing 'first messages IDs by channel' map.")
  val map = mutableMapOf<TextChannel, String?>()
  for (guild in jda.guilds) {
    // Not working because of GC overhead error
//    val messageIds = firstSavedMessages(guild.textChannels.map { it.id })
    for (channel in guild.textChannels) {
      val messageIds = firstSavedMessages(listOf(channel.id))
      map[channel] = messageIds[channel.id]
    }
  }
  map
}

val logger by lazy {
  KotlinLogging.logger { }
}

val gson by lazy {
  Gson()
}

val db by lazy {
  val dbUri = URI(System.getenv("CLEARDB_DATABASE_URL"))
  val username = dbUri.getUserInfo().split(":")[0]
  val password = dbUri.getUserInfo().split(":")[1]
  val dbUrl = "jdbc:mysql://" + dbUri.getHost() + dbUri.getPath()
  val connect = Database.connect(url = dbUrl, driver = "com.mysql.jdbc.Driver", user = username, password = password)
  logger.info("Database connection has been established to $dbUrl for user $username.")
//  createMissingSchemas()
  connect
}

val server by lazy {
  start {
    it.serverConfig {
      it.baseDir(BaseDir.find()).env()
    }.handlers { it.get("health", HealthCheckHandler()) }

  }
}

val jda by lazy {
  val jda = JDABuilder(AccountType.BOT)
      .addEventListener(OwnerCommandListener())
      .setToken(System.getenv("BOT_TOKEN") ?: throw Exception("Token wasn't populated."))
      .build()
  logger.info("JDA token has been populated successfully.")
  jda.awaitReady()
}

fun main(args: Array<String>) {
  try {
    gson
    server
    db
    jda
    firstMessageByChannel
    lastMessageByChannel
  } catch (e: Throwable) {
    e.printStackTrace()
    server.stop()
    System.exit(-1)
  }

  GlobalScope.launch {
    while (true) {
      delay(60000)
      startDiscordPoller()
    }
  }
}

suspend fun startDiscordPoller() {
  for (guild in jda.guilds) {
    for (channel in guild.textChannels
        .filter { PermissionUtil.checkPermission(it, it.guild.selfMember, Permission.MESSAGE_READ) }) {
      if (!lastMessageByChannel.containsKey(channel)) {
        lastMessageByChannel[channel] = null
      }
      if (!firstMessageByChannel.containsKey(channel)) {
        firstMessageByChannel[channel] = null
      }

      backoffRetry(name = "${guild.name}/${channel.name}", initialDelay = 1000, factor = 2.0) {
        val oldMessages = uploadOldMessages(channel)
        val newMessages = uploadNewMessages(channel)
        val uploadedMessages = oldMessages.await() + newMessages.await()
        if (uploadedMessages > 0) {
          logger.info("[${now()}] Uploaded ${uploadedMessages} messages for channel ${guild.name}/${channel.name}.")
        }
      }
    }
  }
}

suspend fun uploadNewMessages(channel: TextChannel) = coroutineScope {
  var latestSavedMessageId = lastMessageByChannel[channel]
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
    lastMessageByChannel[channel] = latestSavedMessageId
    newMessagesUploaded
  }
}

suspend fun uploadOldMessages(channel: TextChannel) = coroutineScope {
  var firstSavedMessageId = firstMessageByChannel[channel] ?: channel.getLatestMessageIdSafe()
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
    firstMessageByChannel[channel] = firstSavedMessageId
    newMessagesUploaded
  }
}

fun createMissingSchemas() {
  transaction {
    println("Create missing schemas.")
    SchemaUtils.createMissingTablesAndColumns(UserMessage)
  }
}
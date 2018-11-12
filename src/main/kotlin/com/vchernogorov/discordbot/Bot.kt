package com.vchernogorov.discordbot

import com.google.gson.Gson
import kotlinx.coroutines.*
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
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

val gson by lazy {
  Gson()
}
val db by lazy {
  val dbUri = URI(System.getenv("CLEARDB_DATABASE_URL"))
  val username = dbUri.getUserInfo().split(":")[0]
  val password = dbUri.getUserInfo().split(":")[1]
  val dbUrl = "jdbc:mysql://" + dbUri.getHost() + dbUri.getPath()
  val connect = Database.connect(url = dbUrl, driver = "com.mysql.jdbc.Driver", user = username, password = password)
  println("Database connection has been established to $dbUrl for user $username.")
  createMissingSchemas()
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
  println("JDA token has been populated successfully.")
  GlobalScope.launch {
    while (true) {
      startDiscordPoller()
    }
  }
  jda
}

fun main(args: Array<String>) {
  try {
    gson
    server
    db
    jda
  } catch (e: Throwable) {
    e.printStackTrace()
    println("Stoping ratpack...")
    server.stop()
    System.exit(-1)
  }
}

suspend fun startDiscordPoller() {
  for (guild in jda.guilds) {
    for (channel in guild.textChannels
        .filter { PermissionUtil.checkPermission(it, it.guild.selfMember, Permission.MESSAGE_READ) }) {
      backoffRetry(name = "${guild.name}/${channel.name}", initialDelay = 1000, factor = 2.0) {
        val oldMessages = uploadOldMessages(channel)
        val newMessages = uploadNewMessages(channel)
        val uploadedMessages = oldMessages.await() + newMessages.await()
        if (uploadedMessages > 0) {
          println("[${now()}] Uploaded ${uploadedMessages} messages for channel ${guild.name}/${channel.name}.")
        }
      }
    }
  }
}

suspend fun uploadNewMessages(channel: TextChannel) = coroutineScope {
  var latestSavedMessageId = latestSavedMessage(channel.id)?.get(UserMessage.id)
      ?: channel.getLatestMessageIdSafe()
  val latestMessageId = channel.getLatestMessageIdSafe()
  async {
    var newMessagesUploaded = 0
    while (true) {
      val newMessages = if (latestSavedMessageId != null && latestSavedMessageId != latestMessageId) {
        channel.getHistoryAfter(latestSavedMessageId, 100).complete().retrievedHistory
      } else {
        break
      }
      uploadMessages(newMessages)
      latestSavedMessageId = newMessages.maxBy { it.creationTime }?.id
      newMessagesUploaded += newMessages.size
    }
    newMessagesUploaded
  }
}

suspend fun uploadOldMessages(channel: TextChannel) = coroutineScope {
  var firstSavedMessageId = firstSavedMessage(channel.id)?.get(UserMessage.id)
      ?: channel.getLatestMessageIdSafe()
  async {
    var newMessagesUploaded = 0
    while (true) {
      val newMessages = channel.getHistoryBefore(firstSavedMessageId, 100).complete().retrievedHistory
      if (newMessages.isEmpty()) {
        break
      }
      uploadMessages(newMessages)
      firstSavedMessageId = newMessages.minBy { it.creationTime }?.id
      newMessagesUploaded += newMessages.size
    }
    newMessagesUploaded
  }
}


fun createMissingSchemas() {
  transaction {
    println("Create missing schemas.")
    SchemaUtils.createMissingTablesAndColumns(UserMessage)
  }
}
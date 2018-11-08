package com.vchernogorov.discordbot

import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ratpack.server.BaseDir
import ratpack.server.RatpackServer.start
import java.io.IOException
import java.net.URI
import java.time.Instant.now
import java.util.concurrent.Executors
import kotlin.concurrent.fixedRateTimer

val gson = Gson()
val db: Database by lazy {
  val dbUri = URI(System.getenv("CLEARDB_DATABASE_URL"))
  val username = dbUri.getUserInfo().split(":")[0]
  val password = dbUri.getUserInfo().split(":")[1]
  val dbUrl = "jdbc:mysql://" + dbUri.getHost() + dbUri.getPath()
  val connect = Database.connect(url = dbUrl, driver = "com.mysql.jdbc.Driver", user = username, password = password)
  println("Database connection has been established to $dbUrl for user $username.")
  connect
}

fun main(args: Array<String>) {
  start {
    it.serverConfig {
      it.baseDir(BaseDir.find()).env()
    }
  }

  db
  transaction {
    println("Create missing schemas.")
    SchemaUtils.createMissingTablesAndColumns (UserMessage)
  }

  val token = System.getenv("BOT_TOKEN")
  val jda = try {
    val jda = JDABuilder(AccountType.BOT)
        .addEventListener(OwnerCommandListener())
        .setToken(token ?: throw Exception("Token wasn't populated."))
        .build()
    println("JDA token has been populated successfully.")
    jda
  } catch (e: Exception) {
    e.printStackTrace()
    System.exit(-1)
    return
  }

  fixedRateTimer(null, false, 1000, 10000) {
    for (guild in jda.guilds) {
      for (channel in guild.textChannels) {
//        println("[${now()}] Start gathering messages from channel ${guild.name}/${channel.name}. " +
//            "Looking for last saved message...")
        var lastSavedMessageId = lastSavedMessage(channel.id)?.get(UserMessage.id)
            ?: channel.getLatestMessageIdSafe()
        GlobalScope.launch {
          retryIO {
            val newMessages = channel.gatherUserMessages(channel, lastSavedMessageId)
            if (newMessages.isEmpty()) {
              println("[${now()}] Channel ${guild.name}/${channel.name} is up to date.")
              return@retryIO
            }
            uploadMessages(newMessages)
            lastSavedMessageId = newMessages.minBy { it.creationTime }?.id
            println("[${now()}] Channel ${guild.name}/${channel.name} has ${newMessages.size} new messages. " +
                "Last message id=$lastSavedMessageId")
          }
        }
      }
    }
  }
}

fun TextChannel.gatherUserMessages(channel: TextChannel, currentMessageId: String?): List<Message> {
  return if (currentMessageId != null) {
    channel.getHistoryBefore(currentMessageId, 100).complete().retrievedHistory
  } else {
    emptyList()
  }
}

suspend fun retryIO(
    times: Int = Int.MAX_VALUE,
    initialDelay: Long = 100, // 0.1 second
    maxDelay: Long = 1000,    // 1 second
    factor: Double = 2.0,
    block: suspend () -> Unit) {
  var attempt = 0
  var currentDelay = initialDelay
  repeat(times - 1) {
    try {
      return block()
    } catch (e: IOException) {
      attempt++
      println("Retry attempt number $attempt after ${currentDelay/1000.0} seconds.")
      // you can log an error here and/or make a more finer-grained
      // analysis of the cause to see if retry is needed
    }
    delay(currentDelay)
    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
  }
  block() // last attempt
}
package com.vchernogorov.discordbot

import com.google.gson.Gson
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import ratpack.server.BaseDir
import ratpack.server.RatpackServer.start
import java.io.File
import java.time.Instant.now
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.jetbrains.exposed.sql.*
import java.net.URI

val gson = Gson()
val scheduledExecutor = Executors.newScheduledThreadPool(1)
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

  try {
    var mutex = true
    scheduledExecutor.scheduleAtFixedRate({
      if (mutex) {
        mutex = false
        try {
          for (guild in jda.guilds) {
            println("\n[${now()}] Start gathering messages from guild ${guild.name}.")
            for (channel in guild.textChannels) {
              println("[${now()}] Start gathering messages from channel ${guild.name}/${channel.name}. " +
                  "Looking for last saved message...")
              var lastSavedMessageId = lastSavedMessage(channel.id)?.get(UserMessage.id)
                  ?: channel.getLatestMessageIdSafe()
              while (true) {
                val newMessages = channel.gatherUserMessages(channel, lastSavedMessageId)
                if (newMessages.isEmpty()) {
                  println("[${now()}] Channel ${guild.name}/${channel.name} is up to date.")
                  break
                }
                uploadMessages(newMessages)
                lastSavedMessageId = newMessages.minBy { it.creationTime }?.id
                println("[${now()}] Channel ${guild.name}/${channel.name} has ${newMessages.size} new messages. " +
                    "Last message id=$lastSavedMessageId")
              }
            }
          }
        } catch (e: Exception) {
          e.printStackTrace()
        } finally {
          mutex = true
        }
      } else {
        println("Gathering job is already in process...")
      }
    }, 10000, 10000, TimeUnit.MILLISECONDS)
  } catch (e: Exception) {
    e.printStackTrace()
  }
}

fun TextChannel.gatherUserMessages(channel: TextChannel, currentMessageId: String?): List<Message> {
  return if (currentMessageId != null) {
    channel.getHistoryBefore(currentMessageId, 100).complete().retrievedHistory
  } else {
    emptyList()
  }
}
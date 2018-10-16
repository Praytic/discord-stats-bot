package com.vchernogorov.discordbot

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import ratpack.server.BaseDir
import ratpack.server.RatpackServer.start
import java.io.File
import java.util.stream.Collectors


val gson = Gson()
var token: String? = null
val jda: JDA by lazy {
  JDABuilder(AccountType.BOT)
      .addEventListener(OwnerCommandListener())
      .setToken(token ?: throw Exception("Token wasn't populated."))
      .build()
}

fun main(args: Array<String>) {
  start {
    it.serverConfig {
      it.baseDir(BaseDir.find()).env()
    }
  }
  token = System.getenv("BOT_TOKEN")
  jda
}

inline fun <reified T> Gson.fromJson(json: String) =
    this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun MessageChannel.getMessages(limit: Long) =
    this.iterableHistory.stream().limit(limit).collect(Collectors.toList())

fun MessageReceivedEvent.send(message: String) =
    textChannel.sendMessage(net.dv8tion.jda.core.MessageBuilder().append(message).build())

fun File.createEverything(): File {
  parentFile.mkdirs()
  createNewFile()
  return this
}
package com.vchernogorov.discordbot

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.io.File
import java.lang.IllegalStateException
import java.util.stream.Collectors

inline fun <reified T> Gson.fromJson(json: String) =
    this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun MessageChannel.getMessages(limit: Long) =
    this.iterableHistory.stream().limit(limit).collect(Collectors.toList())

fun MessageReceivedEvent.send(message: String) =
    textChannel.sendMessage(net.dv8tion.jda.core.MessageBuilder().append(message).build())

fun File.createEverything(): File {
  if (!exists()) {
    parentFile.mkdirs()
    createNewFile()
  }
  return this
}

fun MessageChannel.getLatestMessageIdSafe(): String? {
  return try {
    latestMessageId
  } catch (e: IllegalStateException) {
    null
  }
}
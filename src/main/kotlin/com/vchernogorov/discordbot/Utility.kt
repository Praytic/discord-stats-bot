package com.vchernogorov.discordbot

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
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

suspend fun backoffRetry(
    name: String? = null,
    times: Int = Int.MAX_VALUE,
    initialDelay: Long = 1000 * 60,
    maxDelay: Long = Long.MAX_VALUE,
    factor: Double = 2.0,
    block: suspend () -> Unit) {
  var attempt = 0
  var currentDelay = initialDelay
  repeat(times - 1) {
    try {
      return block()
    } catch (e: Exception) {
      attempt++
      e.printStackTrace()
      println("Retrying job [$name] after ${currentDelay/1000} seconds.")
    }
    delay(currentDelay)
    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
  }
  block()
}
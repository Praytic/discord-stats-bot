package com.vchernogorov.discordbot

import com.vchernogorov.discordbot.task.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.lang.IllegalArgumentException

class OwnerCommandListener : ListenerAdapter() {

  val tasks = mapOf(
      Mode.CHANNEL_STATS to ChannelStatsTask(),
      Mode.USER_STATS to UserStatsTask()
  )
  val ownerId = 278592046124761088L

  override fun onMessageReceived(event: MessageReceivedEvent) {
    if (event.author.isBot && event.author.idLong != ownerId) {
      return
    }

    val commands = event.message.contentRaw.split(" ")
    val params = commands.drop(1).toTypedArray()
    val command = commands[0]
    val mode: Mode = try {
      Mode.valueOf(command)
    } catch (e: IllegalArgumentException) {
      Mode.UNDEFINED
    }
    try {
      tasks[mode]?.execute(event, *params)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
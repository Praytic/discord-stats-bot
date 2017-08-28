package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.UserStat
import com.vchernogorov.discordbot.createEverything
import com.vchernogorov.discordbot.gson
import com.vchernogorov.discordbot.loadAllMessages
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.io.File

class UserStatsTask : MessagesStatsTask() {

  override fun execute(event: MessageReceivedEvent, vararg params: String) = with(event.guild) {
    val messages = loadAllMessages(messagesFolderName)
    val textChannels = if (params.isEmpty()) textChannels else params
        .map { name -> getTextChannelsByName(name, true) }
        .flatten()
        .toSet()
    (if (params.isEmpty()) members else params
        .map { name ->
          listOf<Member>()
              .union(getMembersByName(name, true))
              .union(getMembersByNickname(name, true))
              .union(getMembersByEffectiveName(name, true))
        }
        .flatten())
        .map { member ->
          textChannels
              .map { channel ->
                channel to member.user to messages
                    .filter { it.creatorId == member.user.id && it.channelId == channel.id }
              }
        }
        .flatten()
        .forEach { pair ->
          val key = pair.first
          val value = pair.second
          val channel = key.first
          val user = key.second
          File("$statsFolderName/${channel.id}/${user.id}.json")
              .createEverything()
              .bufferedWriter()
              .use { out ->
                println("Creating $statsFolderName/${channel.id}/${user.id}.json.")
                val userStat = UserStat(
                    user.name,
                    totalMessages(value),
                    avgMessageLength(value),
                    avgMessagesPerActiveDay(value),
                    avgMessagesPerDay(value),
                    getMember(user).joinDate,
                    totalOccurrences(value, "хуйню", "хуйни"))
                out.write(gson.toJson(userStat))
              }
        }
  }
}
package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.UserMessage
import com.vchernogorov.discordbot.getMessages
import com.vchernogorov.discordbot.gson
import com.vchernogorov.discordbot.send
import kotlinx.coroutines.experimental.async
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.utils.PermissionUtil
import java.io.File
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock

class GatherStatsTask : MessageTask() {

  val lock: ReentrantLock = ReentrantLock()

  override fun execute(event: MessageReceivedEvent, vararg params: String) {
    if (lock.isHeldByCurrentThread) {
      gatherStatsInProcess(event)
      return
    }
    if (lock.tryLock()) try {
      gatherStats(event)
    } finally {
      lock.unlock();
    } else {
      gatherStatsInProcess(event)
    }
  }

  private fun gatherStats(event: MessageReceivedEvent) {
    event.guild.textChannels.forEach { channel ->
      if (!PermissionUtil.checkPermission(channel, event.guild.selfMember, Permission.MESSAGE_READ)) {
        throw Exception("Unable to read ${channel} due to lack of permissions.")
      }
      if (File("$messagesFolderName/${channel.id}.json").exists()) {
        return
      }
      if (!File(messagesFolderName).isDirectory) {
        File(messagesFolderName).mkdir()
      }
      File("$messagesFolderName/${channel.id}.json").bufferedWriter().use { out ->
        println("Creating $messagesFolderName/${channel.id}.json.")
        try {
          event.send("Gathering messages for ${channel.id}")
          val now = Instant.now()
          val messages = channel.getMessages(1_000_000L).map { UserMessage(it) }
          val duration = Instant.now().epochSecond - now.epochSecond
          event.send("${messages.size} messages gathered for channel ${channel} in ${duration} seconds.")
          out.write(gson.toJson(messages))
        } catch (e: PermissionException) {
          throw Exception("Exception was thrown while reading ${channel} channel.")
        }
      }
    }
  }

  private fun gatherStatsInProcess(event: MessageReceivedEvent) =
      event.send("Gathering stats process has been already started.")
}
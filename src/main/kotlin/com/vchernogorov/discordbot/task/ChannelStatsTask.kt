package com.vchernogorov.discordbot.task

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class ChannelStatsTask : MessagesStatsTask() {

    override fun execute(event: MessageReceivedEvent, vararg params: String) = with(event.guild) {
        //    textChannels.forEach { channel ->
//      try {
//        File("$messagesFolderName/${channel.id}.json").bufferedReader().use { it ->
//          val messages = gson.fromJson<List<UserMessage>>(it.readText())
//              ?: throw Exception("Channel ${channel} has broken data file.")
//
//          System.out.print("${channel.name}:\n" +
//              "Total messages: ${totalMessages(messages)}\n" +
//              "Avg. messages per day: ${avgMessagesPerActiveDay(messages).toInt()}\n"
//          )
//        }
//      } catch(e: Exception) {
//        e.printStackTrace()
//      }
//    }
    }
}
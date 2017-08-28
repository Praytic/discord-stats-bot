package com.vchernogorov.discordbot

import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class OverwatchCommandHandler {

  fun handle(event: MessageReceivedEvent, command: String) {

    val arguments = command
        .split(" ")
        .filter { it.isNotEmpty() }
        .filter { !it.startsWith("=") }
    val battletags = arguments
        .filter { !it.startsWith("@") && it.contains("-") }
    val message: Message?

    if (event.message.mentionsEveryone()) {
      message = handleAllRankRequest(event)
    } else if (arguments.contains("rmbind")) {
      message = handleRemoveBind(event)
    } else if (arguments.contains("bind")) {
      message = handleBind(event, battletags)
    } else if (arguments.contains("me")) {
      message = handleMe(event)
    } else if (event.message.mentionedUsers.isNotEmpty()) {
      message = handleRankRequest(event)
    } else {
      message = handleStatus(arguments)
    }

    if (message != null) {
      event.channel.sendMessage(message).queue({
        event.message.delete().queue()
      })
    }
  }

  fun handleStatus(status: List<String>): Message {
    Bot.jda.presence.game = Game.of(status.joinToString { " " })
    val text = "Bot status was set to $status."
    println(text)
    return MessageBuilder().append(text).build()
  }

  fun handleAllRankRequest(event: MessageReceivedEvent): Message? {
    val messageBuilder = MessageBuilder()
    val errorBuilder = MessageBuilder()
    var counter = 1
    Bot.battletagRanks.entries
        .sortedByDescending { it.value }
        .forEach {
          try {
            getRankMessage(event, it.key, messageBuilder, counter++).appendln()
          } catch (ex: IllegalStateException) {
            errorBuilder.append(ex.message).appendln()
            ex.printStackTrace()
          }
        }
    if (!errorBuilder.isEmpty) {
      event.channel.sendMessage(errorBuilder.build()).queue()
    }
    return if (messageBuilder.isEmpty) null else messageBuilder.build()
  }

  fun handleRankRequest(event: MessageReceivedEvent): Message? {
    val messageBuilder = MessageBuilder()
    val errorBuilder = MessageBuilder()
    var counter = 1
    event.message.mentionedUsers
        .sortedByDescending { Bot.battletagRanks[it] }
        .forEach {
          try {
            getRankMessage(event, it, messageBuilder, counter++).appendln()
          } catch (ex: IllegalStateException) {
            errorBuilder.append(ex.message).appendln()
            ex.printStackTrace()
          }
        }
    if (!errorBuilder.isEmpty) {
      event.channel.sendMessage(errorBuilder.build()).queue()
    }
    return if (messageBuilder.isEmpty) null else messageBuilder.build()
  }

  fun handleMe(event: MessageReceivedEvent): Message {
    return getRankMessage(event, event.author, MessageBuilder()).build()
  }

  fun handleBind(event: MessageReceivedEvent, battletags: List<String>):
      Message {
    val battletag = battletags.singleOrNull() ?:
        throw IllegalArgumentException("Provide one battletag to bind it.")
    val mention = event.message.mentionedUsers.singleOrNull() ?:
        throw IllegalArgumentException("Provide one user for battletag " +
            "binding.")
    Bot.bindedBattletags.put(mention, battletag)
    val text = "${event.author.asMention} binded $battletag to ${mention.name}."
    println(text)
    return MessageBuilder().append(text).build()
  }

  fun handleRemoveBind(event: MessageReceivedEvent): Message {
    val mentions = event.message.mentionedUsers
    val removedBindings = mentions
        .map { user ->
          Bot.battletagRanks.remove(user)
          Bot.bindedBattletags.remove(user)
          user.name
        }
        .filterNotNull()
    val text = "${event.author.asMention} removed bindings for: " +
        "$removedBindings."
    println(text)
    return MessageBuilder().append(text).build()
  }

  fun getRankMessage(event: MessageReceivedEvent,
                     user: User,
                     messageBuilder: MessageBuilder,
                     placement: Int = 0): MessageBuilder {
    val rankEmotes = event.guild.emotes
        .filter { it.name.contains("owrank") }
        .sortedBy { it.name }
    val rank = Bot.battletagRanks[user] ?:
        throw IllegalStateException("${event.member.asMention} you should bind a " +
            "battletag first. Type command `=ow bind @${user.name} " +
            "Battletag-9999`")

    val rankEmote = when (rank) {
      in 1..1499 -> rankEmotes[7]
      in 1500..1999 -> rankEmotes[6]
      in 2000..2499 -> rankEmotes[5]
      in 2500..2999 -> rankEmotes[4]
      in 3000..3499 -> rankEmotes[3]
      in 3500..3999 -> rankEmotes[2]
      in 4000..4999 -> rankEmotes[1]
      else -> throw IllegalStateException("Could not get rank for ${user
          .name}.")
    }
    val textMessage = "${user.name} as `${Bot.bindedBattletags[user]}`: "
    if (placement != 0) {
      messageBuilder.append("$placement. ")
    }
    messageBuilder
        .append(textMessage)
        .append(rankEmote)
        .append("`$rank`")
    println(textMessage + rank)
    return messageBuilder
  }
}
package com.vchernogorov.discordbot.task

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

abstract class MessageTask {

  val messagesFolderName = "messages"

  abstract fun execute(event: MessageReceivedEvent, vararg params: String)
}
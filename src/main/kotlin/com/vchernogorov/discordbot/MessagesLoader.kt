package com.vchernogorov.discordbot

import java.io.File

fun loadMessages(path: String): List<UserMessage> {
  File(path).bufferedReader().use {
    return gson.fromJson<List<UserMessage>>(it.readText())
  }
}

fun loadAllMessages(path: String): List<UserMessage> {
  val allUserMessages = mutableListOf<UserMessage>()
  File(path).listFiles().forEach {
    allUserMessages.addAll(loadMessages(it.path))
  }
  return allUserMessages
}
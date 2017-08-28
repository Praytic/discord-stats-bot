package com.vchernogorov.discordbot

import khttp.get
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.User
import org.json.JSONException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

class Bot {

  companion object {
    val bindedBattletags = ConcurrentHashMap<User, String>()
    val battletagRanks = ConcurrentHashMap<User, Int>()
    val jda = JDABuilder(AccountType.BOT)
        .addEventListener(BotListener())
        .setToken("MzE3Njg2NDA2MzU1MDkxNDU2.DAwpLw.0dfNNvkX08I0SZ5kGQkNrdn2xv8")
        .buildAsync()
  }

  init {
    Timer().scheduleAtFixedRate(timerTask {
      println("Refresh stats for ${bindedBattletags.keys}.")
      bindedBattletags.forEach {
        try {
          TimeUnit.SECONDS.sleep(1)
          val rank = get("https://owapi.net/api/v3/u/${it.value}/stats")
              .jsonObject.getJSONObject("eu")
              .getJSONObject("stats")
              .getJSONObject("competitive")
              .getJSONObject("overall_stats")
              .getInt("comprank")
          battletagRanks.put(it.key, rank)
        } catch (ex: JSONException) {
          jda.presence.game = Game.of("Bad")
        }
      }
    }, Date(), TimeUnit.MINUTES.toMillis(1))
  }
}
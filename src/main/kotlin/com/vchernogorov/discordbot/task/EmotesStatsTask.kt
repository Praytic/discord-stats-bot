package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.UserStatsArgs
import com.xenomachina.argparser.ArgParser
import net.dv8tion.jda.core.entities.Emote
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.OffsetDateTime

abstract class EmotesStatsTask : MessagesStatsTask() {

}

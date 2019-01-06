package com.vchernogorov.discordbot.task

import com.vchernogorov.discordbot.Mode
import com.vchernogorov.discordbot.MemberStat
import com.vchernogorov.discordbot.send
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.io.File

fun handle(event: MessageReceivedEvent, vararg params: String) = with(event.guild) {
    val printMode = Mode.valueOf(params[0])
    val textChannels = if (params.isEmpty()) textChannels else params
            .map { name -> getTextChannelsByName(name, true) }
            .flatten()
            .toSet()
    val members = if (params.isEmpty()) members else params
            .map { name ->
                listOf<Member>()
                        .union(getMembersByName(name, true))
                        .union(getMembersByNickname(name, true))
                        .union(getMembersByEffectiveName(name, true))
            }
            .flatten()
    when (printMode) {
        Mode.MEMBER_STATS -> {
            var message = ""
            message += "```css\n" + "[Top messages]\n"
            members.forEachIndexed { i, member ->
                textChannels.forEach { channel ->
                    val userStats = mutableListOf<MemberStat>()
                    File("stats/${channel.id}/${member.user.id}.json")
                            .bufferedReader()
                            .use { _in ->
                                //                val userStat = gson.fromJson<MemberStat>(_in.readText())
//                userStats += userStat
                            }
//           val aggregatedUserStat = MemberStat(
//             userStats.first().user,
//             userStats.map { it.activePeriod }.reduceRight {acc, it -> acc.plus(it) },
//             userStats.map { it.activeDays }.flatten().toSet(),
//             userStats.map { it.messages }.flatten().toSet(),
//             userStats.map { it.messages },
//             avgMessagesPerDay(value),
//             getMember(user).joinDate,
//             totalOccurrences(value, "хуйню", "хуйни"))
                    message += "$i. "
                }
            }

            event.send(message)
        }
        Mode.CHANNEL_STATS -> {
            var message = ""

            event.send(message)
        }
        else -> throw Exception("Wrong print mode: $printMode.")
    }
    Unit
}

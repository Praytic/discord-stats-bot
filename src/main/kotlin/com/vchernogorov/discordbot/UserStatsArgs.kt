package com.vchernogorov.discordbot

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel

class UserStatsArgs(parser: ArgParser, guild: Guild) {

    companion object {
        val LIMIT_PRIMARY_RESULTS_MAX = 50
        val LIMIT_SECONDARY_RESULTS_MAX = 10
        val LIMIT_USERS_BY_MESSAGES_MAX = Int.MAX_VALUE / 2
    }

    val limitSecondaryResults by parser.storing(
            "--limitSecondaryResults",
            help = "how much secondary results should be included in top stats. " +
                    "Example: --limitSecondaryResults=3"
    ) { toInt() }.default(3)
            .addValidator {
                value <= LIMIT_SECONDARY_RESULTS_MAX && value > 0
            }

    val limitPrimaryResults by parser.storing(
            "--limitPrimaryResults",
            help = "how much primary results should be included in top stats. " +
                    "Example: --limitPrimaryResults=10"
    ) { toInt() }
            .default(10)
            .addValidator {
                value <= LIMIT_PRIMARY_RESULTS_MAX && value > 0
            }

    val limitUsersByMessages by parser.storing(
            "--limitUsersByMessages",
            help = "indicates how much messages user has to have in order to be included in stats. " +
                    "Example: --limitUsersByMessages=1000"
    ) { toLong() }
            .default(1000)
            .addValidator {
                value < LIMIT_USERS_BY_MESSAGES_MAX && value > 0
            }

    val members by parser.storing(
            "--members",
            help = "what users should be included in stats. " +
                    "Example: --members=user1,user2"
    ) {
        this.split(",").map {
            listOf<Member>()
                    .union(guild.getMembersByName(it, true))
                    .union(guild.getMembersByNickname(it, true))
                    .union(guild.getMembersByEffectiveName(it, true))
                    .singleOrNull() ?: throw Exception("Unable to uniquely identify user with name $it.")
        }
    }.default(guild.members)

    val channels by parser.storing(
            "--channels",
            help = "what channels should be included in stats. " +
                    "Example: --channels=channel1,channel2"
    ) {
        this.split(",").map {
            guild.getTextChannelsByName(it, true)
                    .singleOrNull() ?: throw Exception("Unable to uniquely identify channel with name $it.")
        }
    }.default(guild.textChannels)

    val tail by parser.flagging(
            "--tail",
            help = "when active, tail results will be shown instead of top results. " +
                    "Example: --tail"
    ).default(false)
}
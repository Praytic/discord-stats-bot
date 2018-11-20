package com.vchernogorov.discordbot

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel

class UserStatsArgs(parser: ArgParser, channel: TextChannel) {
    val limitSecondaryResults by parser.storing(
            "--limitSecondaryResults",
            help = "how much secondary results should be included in top stats. " +
                    "Example: --limitSecondaryResults=3"
    ) { toInt() }.default(3)

    val limitPrimaryResults by parser.storing(
            "--limitPrimaryResults",
            help = "how much primary results should be included in top stats. " +
                    "Example: --limitPrimaryResults=10"
    ) { toInt() }.default(10)

    val limitUsersByMessages by parser.storing(
            "--limitUsersByMessages",
            help = "indicates how much messages user has to have in order to be included in stats. " +
                    "Example: --limitUsersByMessages=1000"
    ) { toLong() }.default(1000)

    val members by parser.storing(
            "--members",
            help = "what users should be included in stats. " +
                    "Example: --members=user1,user2"
    ) {
        this.split(",").map {
            listOf<Member>()
                    .union(channel.guild.getMembersByName(it, true))
                    .union(channel.guild.getMembersByNickname(it, true))
                    .union(channel.guild.getMembersByEffectiveName(it, true))
                    .singleOrNull() ?: throw Exception("Unable to uniquely identify user with name $it.")
        }
    }.default(channel.guild.members)

    val channels by parser.storing(
            "--channels",
            help = "what channels should be included in stats. " +
                    "Example: --channels=channel1,channel2"
    ) {
        this.split(",").map {
            channel.guild.getTextChannelsByName(it, true)
                    .singleOrNull() ?: throw Exception("Unable to uniquely identify channel with name $it.")
        }
    }.default(channel.guild.textChannels)

    val tail by parser.flagging(
            "--tail",
            help = "when active, tail results will be shown instead of top results. " +
                    "Example: --tail"
    ).default(false)
}
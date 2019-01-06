package com.vchernogorov.discordbot.args

import com.vchernogorov.discordbot.Mode
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import net.dv8tion.jda.core.entities.Member

/**
 * Arguments for [MemberStatsHandler].
 */
class MemberStatsArgs(parser: ArgParser, member: Member, command: Mode) : StatsArgs(command) {

    val months by parser.storing(
            "--months",
            help = "add period restriction for specified months (year parameter is required to be specified). " +
                    "Example: --months=1,3,12"
    ) { toInt() }

    val years by parser.storing(
            "--years",
            help = "add period restriction for specified years. " +
                    "Example: --years=2017,2018"
    ) { toInt() }

    val channels by parser.storing(
            "--channels",
            help = "what channels should be included in stats. " +
                    "Example: --channels=channel1,channel2"
    ) {
        this.split(",").map {
            member.guild.getTextChannelsByName(it, true)
                    .singleOrNull() ?: throw Exception("Unable to uniquely identify channel with name $it.")
        }
    }.default(member.guild.textChannels)
}
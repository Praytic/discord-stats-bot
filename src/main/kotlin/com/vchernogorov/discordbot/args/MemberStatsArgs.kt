package com.vchernogorov.discordbot.args

import com.vchernogorov.discordbot.Mode
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import net.dv8tion.jda.core.entities.Member

/**
 * Arguments for [MemberStatsHandler].
 */
class MemberStatsArgs(parser: ArgParser, member: Member, command: Mode) : StatsArgs(command) {

    val compare by parser.storing(
            "--compare",
            help = "specify members for stats comparison. " +
                    "Example: --compare=praytic,praytic2"
    ) {
        this.split(",").map {
            val possibleMembers = listOf<Member>()
                    .union(member.guild.getMembersByName(it, true))
                    .union(member.guild.getMembersByNickname(it, true))
                    .union(member.guild.getMembersByEffectiveName(it, true))
            possibleMembers.singleOrNull() ?: throw Exception("Unable to uniquely identify user with name $it. Possible members: $possibleMembers")
        }
    }.default(emptyList())

    val member by parser.storing(
            "--member",
            help = "specify member for whom you want to export stats. " +
                    "Example: --member=praytic"
    ) {
        listOf<Member>()
                .union(member.guild.getMembersByName(this, true))
                .union(member.guild.getMembersByNickname(this, true))
                .union(member.guild.getMembersByEffectiveName(this, true))
                .singleOrNull() ?: throw Exception("Unable to uniquely identify user with name $this.")
    }.default(member)

    val months by parser.storing(
            "--months",
            help = "add period restriction for specified months (year parameter is required). " +
                    "Example: --months=1,3,12"
    ) {
        this.split(",").map { it.toInt() }
    }.default(emptyList())

    val years by parser.storing(
            "--years",
            help = "add period restriction for specified years. " +
                    "Example: --years=2017,2018"
    ) {
        this.split(",").map { it.toInt() }
    }.default(emptyList())

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

    override fun toString(): String {
        return "--months=$months --years=$years --channels=$channels"
    }
}
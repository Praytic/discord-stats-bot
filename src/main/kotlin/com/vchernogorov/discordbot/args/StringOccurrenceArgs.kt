package com.vchernogorov.discordbot.args

import com.vchernogorov.discordbot.Mode
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member

/**
 * Arguments for [StringOccurrenceHandler].
 */
class StringOccurrenceArgs(parser: ArgParser, guild: Guild, command: Mode) : GuildStatsArgs(parser, guild, command) {

    val strings by parser.storing(
            "--strings",
            help = "Given strings will be searched in guild messages. You can specify one string or several strings" +
                    "using logical operators | and &.\n" +
                    "Example: --strings=one&two|three&four\n" +
                    "--strings=poop"
    ) {
        this.split("|").map {
            it.split("&")
        }
    }
}
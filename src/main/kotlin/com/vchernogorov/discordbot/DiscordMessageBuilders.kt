package com.vchernogorov.discordbot

import com.vchernogorov.discordbot.args.StringOccurrenceArgs
import net.dv8tion.jda.core.MessageBuilder

fun memberStatMessage(memberStat: MemberStat) = MessageBuilder()
        .append("`[Member statistics for ${memberStat.user}]`\n")
        .append("**Total messages:** ${memberStat.messages} messages\n")
        .append("**Average message length:** ${memberStat.messageLengthAvg.toInt()} symbols\n")
        .append("**Messages per day:** ${memberStat.messagesPerDay.toInt()} messages\n")
        .append("**Messages per active day:** ${memberStat.messagesPerActiveDay.toInt()} messages\n")
        .append("**Days in guild:** ${memberStat.daysInGuild} days\n")
        .append("**Active days:** ${memberStat.activeDays.size} days\n")

fun stringOccurrencesMessage(occurrencesByMember: Map<String, Long>, args: StringOccurrenceArgs): MessageBuilder {
    val messageBuilder = MessageBuilder()
            .append("[String occurrence stats for ${args.strings.map {
                it.joinToString(separator = "&") }.joinToString(separator = "|")
            }]\n")

    occurrencesByMember
            .toList()
            .sortedByDescending { it.second }
            .forEachIndexed { i, (creatorId, count) ->
                if (args.tail && occurrencesByMember.count() - args.limitPrimaryResults <= i ||
                        !args.tail && args.limitPrimaryResults > i) {
                    messageBuilder
                            .append("${i + 1}. ")
                            .append(creatorId)
                            .append(": `$count`\n")
                }
            }
    return messageBuilder
}


fun memberStatCompareMessage(memberStat: MemberStat, memberStats: List<MemberStat>): MessageBuilder {
    val allStats = memberStats + memberStat
    val messageBuilder = MessageBuilder()
            .append("`[Members' statistics comparison for ${memberStat.user} and ${memberStats.map { it.user }.joinToString(separator = ",")}]`\n")

    genericStatCompareMessage("Total messages", messageBuilder, allStats, memberStat, "messages") { it.messages }
    genericStatCompareMessage("Average message length", messageBuilder, allStats, memberStat, "symbols") { it.messageLengthAvg.toInt() }
    genericStatCompareMessage("Messages per day", messageBuilder, allStats, memberStat, "messages") { it.messagesPerDay.toInt() }
    genericStatCompareMessage("Messages per active day", messageBuilder, allStats, memberStat, "messages") { it.messagesPerActiveDay.toInt() }
    genericStatCompareMessage("Days in guild", messageBuilder, allStats, memberStat, "days") { it.daysInGuild }
    genericStatCompareMessage("Active days", messageBuilder, allStats, memberStat, "days") { it.activeDays.size }

    return messageBuilder
}

private fun genericStatCompareMessage(
        title: String,
        messageBuilder: MessageBuilder,
        allStats: List<MemberStat>,
        memberStat: MemberStat,
        postfix: String,
        getField: (MemberStat) -> Int) {
    messageBuilder.append("**$title:**\n")
    allStats.sortedByDescending {
        getField(it)
    }.forEachIndexed { i, compareStat ->
        val comparisonResult = if (getField(compareStat) > getField(memberStat)) {
            "(+${getField(compareStat) - getField(memberStat)} $postfix)"
        } else if (getField(compareStat) < getField(memberStat)) {
            "(-${getField(memberStat) - getField(compareStat)} $postfix)"
        } else {
            ""
        }
        messageBuilder.append("${i + 1}. ${compareStat.user}: ${getField(compareStat)} $comparisonResult\n")
    }
}
package com.vchernogorov.discordbot

import net.dv8tion.jda.core.MessageBuilder

fun memberStatMessage(memberStat: MemberStat) = MessageBuilder()
        .append("`[Member statistics for ${memberStat.user}]`\n")
        .append("**Total messages:** ${memberStat.messages} messages\n")
        .append("**Average message length:** ${memberStat.messageLengthAvg.toInt()} symbols\n")
        .append("**Messages per day:** ${memberStat.messagesPerDay.toInt()} messages\n")
        .append("**Messages per active day:** ${memberStat.messagesPerActiveDay.toInt()} messages\n")
        .append("**Days in guild:** ${memberStat.daysInGuild} days\n")
        .append("**Active days:** ${memberStat.activeDays.size} days\n")

fun memberStatCompareMessage(memberStat: MemberStat, memberStats: List<MemberStat>): MessageBuilder {
    val commonException = Exception("Error occurred during member stats comparison. Check logs for details.")

    val totalMessages = with(memberStats.maxBy { it.messages } ?: throw commonException) {
        if (memberStat.messages > this.messages) {
            memberStat.messages - this.messages to memberStat
        } else {
            this.messages - memberStat.messages to this
        }
    }

    val avgMessageLength = with(memberStats.maxBy { it.messageLengthAvg } ?: throw commonException) {
        if (memberStat.messageLengthAvg > this.messageLengthAvg) {
            (memberStat.messageLengthAvg - this.messageLengthAvg).toInt() to memberStat
        } else {
            (this.messageLengthAvg - memberStat.messageLengthAvg).toInt() to this
        }
    }

    val messagesPerDay = with(memberStats.maxBy { it.messagesPerDay } ?: throw commonException) {
        if (memberStat.messagesPerDay > this.messagesPerDay) {
            (memberStat.messagesPerDay - this.messagesPerDay).toInt() to memberStat
        } else {
            (this.messagesPerDay - memberStat.messagesPerDay).toInt() to this
        }
    }

    val messagesPerActiveDay = with(memberStats.maxBy { it.messagesPerActiveDay } ?: throw commonException) {
        if (memberStat.messagesPerActiveDay > this.messagesPerActiveDay) {
            (memberStat.messagesPerActiveDay - this.messagesPerActiveDay).toInt() to memberStat
        } else {
            (this.messagesPerActiveDay - memberStat.messagesPerActiveDay).toInt() to this
        }
    }

    val activeDays = with(memberStats.maxBy { it.activeDays.size } ?: throw commonException) {
        if (memberStat.activeDays.size > this.activeDays.size) {
            memberStat.activeDays.size - this.activeDays.size to memberStat
        } else {
            this.activeDays.size - memberStat.activeDays.size to this
        }
    }

    val daysInGuild = with(memberStats.maxBy { it.daysInGuild } ?: throw commonException) {
        if (memberStat.daysInGuild > this.daysInGuild) {
            memberStat.daysInGuild - this.daysInGuild to memberStat
        } else {
            this.daysInGuild - memberStat.daysInGuild to this
        }
    }

    return MessageBuilder()
                .append("`[Members' statistics comparison for ${memberStat.user} and ${memberStats.map { it.user }.joinToString(separator = ",")}]`\n")
                .append("**Total messages:** ${totalMessages.second.user} (+${totalMessages.first} messages)\n")
                .append("**Average message length:** ${avgMessageLength.second.user} (+${avgMessageLength.first} symbols)\n")
                .append("**Messages per day:** ${messagesPerDay.second.user} (+${messagesPerDay.first} messages)\n")
                .append("**Messages per active day:** ${messagesPerActiveDay.second.user} (+${messagesPerActiveDay.first} messages)\n")
                .append("**Days in guild:** ${daysInGuild.second.user} (+${daysInGuild.first} days)\n")
                .append("**Active days:** ${activeDays.second.user} (+${activeDays.first} days)\n")
}
package com.vchernogorov.discordbot

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

class MyArgs(parser: ArgParser) {
    val fetchDelay by parser.storing(
            "--fetchDelayMillis",
            help = "sets the delay of messages fetching in milliseconds") { toLong() }.default(60 * 1000)

    val createSchemas by parser.flagging(
            "--createSchemas",
            help = "if active, missing schemas will be created on startup"
    ).default(false)

    val backoffRetryDelay by parser.storing(
            "--backoffRetryDelay",
            help = "sets the delay of backoff retry mechanism on failed database request"
    ) { toLong() }.default(10 * 60 * 1000)

    val backoffRetryFactor by parser.storing(
            "--backoffRetryFactor",
            help = "sets the multiplier factor of backoff retry mechanism on failed database request"
    ) { toDouble() }.default(1.0)

    val bulkMessageIdsSelection by parser.flagging(
            "--bulkMessageIdsSelection",
            help = "if active, bot initializer will query all messages at once instead of querying by channels"
    )
}
package com.vchernogorov.discordbot

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import org.slf4j.Logger

class MyArgs(parser: ArgParser) {
    val fetchDelay by parser.storing(
            "--fetchDelayMillis",
            help = "sets the delay of messages fetching in milliseconds") { toLong() }.default(60 * 1000)

    val createSchemas by parser.flagging(
            "--createSchemas",
            help = "if active, missing schemas will be created on startup"
    )

    val backoffRetryDelay by parser.storing(
            "--backoffRetryDelay",
            help = "sets the delay of backoff retry mechanism on failed database request"
    ) { toLong() }.default(10 * 60 * 1000)

    val backoffRetryFactor by parser.storing(
            "--backoffRetryFactor",
            help = "sets the multiplier factor of backoff retry mechanism on failed database request"
    ) { toDouble() }.default(1.0)

    val printErrorsToDiscord by parser.flagging(
            "--printErrorsToDiscord",
            help = "if active, all exceptions will be printed to logs AND discord channel where it occurred"
    )

    val removeOriginalRequest by parser.flagging(
            "--removeOriginalRequest",
            help = "if active, original request message will be removed"
    )

    val chunkSize by parser.storing(
            "--chunkSize",
            help = "sets how much result rows one selection query should contain"
    ) { toInt() }.default(1000)

    val fetchMessages by parser.flagging(
            "--fetchMessages",
            help = "enables loading new and old messages from discord channels to the database"
    )

    fun printArgs(logger: Logger) {
        logger.info("Fetch delay is set to ${fetchDelay / 1000.0} seconds.")
        logger.info("Create schemas on startup is ${if (createSchemas) "enabled" else "disabled"}.")
        logger.info("Backoff retry delay is set to ${backoffRetryDelay / 60000.0} minutes.")
        logger.info("Backoff retry factor is set to $backoffRetryFactor.")
        logger.info("Print errors to discord is ${if (printErrorsToDiscord) "enabled" else "disabled"}.")
        logger.info("Remove original request message is ${if (removeOriginalRequest) "enabled" else "disabled"}.")
        logger.info("Selections result set is limited by $chunkSize size.")
        logger.info("Fetching messages is ${if (fetchMessages) "enabled" else "disabled"}.")
    }
}
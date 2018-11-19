package com.vchernogorov.discordbot

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import mu.KLogger
import org.slf4j.Logger

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

    val hugeTransactions by parser.flagging(
            "--hugeTransactions",
            help = "if active, huge transactions can be made"
    ).default(false)

    val printErrorsToDiscord by parser.flagging(
            "--printErrorsToDiscord",
            help = "if active, all exceptions will be printed to logs AND discord channel where it occurred"
    ).default(false)

    fun printArgs(logger: Logger) {
        logger.info("Fetch delay is set to ${fetchDelay / 1000.0} seconds.")
        logger.info("Create schemas on startup is ${if (createSchemas) "enabled" else "disabled"}.")
        logger.info("Backoff retry delay is set to ${backoffRetryDelay / 60000.0} minutes.")
        logger.info("Backoff retry factor is set to $backoffRetryFactor.")
        logger.info("Huge transactions are ${if (hugeTransactions) "enabled" else "disabled"}.")
        logger.info("Print errors to discord is ${if (printErrorsToDiscord) "enabled" else "disabled"}.")
    }
}
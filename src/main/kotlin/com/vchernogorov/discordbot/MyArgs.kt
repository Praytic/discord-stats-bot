package com.vchernogorov.discordbot

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

class MyArgs(parser: ArgParser) {
  val fetchDelay by parser.storing(
      "-f", "--fetchDelayMillis",
      help = "sets the delay of messages fetching in milliseconds") { toLong() }.default(60 * 1000)

  val createSchemas by parser.flagging(
      "-c", "--createSchemas",
      help = "when active, missing schemas will be created on startup"
  )

  val backoffRetryDelay by parser.storing(
      "-b", "--backoffRetryDelay",
      help = "sets the delay of backoff retry mechanism on failed database request"
  ) { toLong() }.default(10 * 60 * 1000)

}
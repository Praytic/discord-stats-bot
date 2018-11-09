package com.vchernogorov.discordbot

import kotlinx.coroutines.runBlocking
import org.junit.Test as test

class TestSource {
  @test fun retryIOTest() {
    runBlocking {
      retryIO(initialDelay = 100) {
        throw Exception()
      }
    }
  }
}
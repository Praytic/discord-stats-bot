package com.vchernogorov.discordbot

import kotlinx.coroutines.runBlocking
import org.junit.Test as test

class TestSource {
    @test
    fun retryIOTest() {
        runBlocking {
            var att = 0
            backoffRetry(initialDelay = 100) {
                if (att == 2) {
                    assert(true)
                } else {
                    att++
                    throw Exception()
                }
            }
        }
    }
}
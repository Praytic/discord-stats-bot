package com.vchernogorov.discordbot

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import mu.KotlinLogging
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

private val logger = KotlinLogging.logger("Utility")

inline fun <reified T> Gson.fromJson(json: String) =
        this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun MessageReceivedEvent.send(message: String) =
        textChannel.sendMessage(net.dv8tion.jda.core.MessageBuilder().append(message).build()).complete()

fun MessageReceivedEvent.send(messageBuilder: MessageBuilder) =
        textChannel.sendMessage(messageBuilder.build()).complete()

fun MessageChannel.getLatestMessageIdSafe(): String? {
    return try {
        latestMessageId
    } catch (e: IllegalStateException) {
        null
    }
}

suspend fun backoffRetry(
        name: String? = null,
        times: Int = Int.MAX_VALUE,
        initialDelay: Long = 1000 * 60,
        maxDelay: Long = Long.MAX_VALUE,
        factor: Double = 2.0,
        block: suspend () -> Unit) {
    var attempt = 0
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            attempt++
            logger.error(e) { "Retrying job [$name] after ${currentDelay / 1000} seconds." }
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    block()
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator
 * value and each element until [predicate] is true. After [predicate] becomes false, new accumulator will be created.
 * The result value is a list of accumulators divided by [predicate].
 */
public inline fun <S, reified T : S> Iterable<T>.reduceUntil(predicate: (acc: S, T) -> Boolean, operation: (acc: S, T) -> S): List<S> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) throw UnsupportedOperationException("Empty collection can't be reduced.")
    var next: T = iterator.next()
    val newList = mutableListOf<S>()
    while (iterator.hasNext()) {
        var accumulator: S = next
        next = iterator.next()
        while (iterator.hasNext()) {
            if (!predicate(accumulator, next)) {
                accumulator = operation(accumulator, next)
            } else {
                break
            }
            next = iterator.next()
        }
        newList.add(accumulator)
    }
    return newList
}

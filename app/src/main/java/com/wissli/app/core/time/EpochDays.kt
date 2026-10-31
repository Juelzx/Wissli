package com.wissli.app.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime

private const val MAX_CHECK_INTERVAL_MILLIS = 5 * 60 * 1000L

/**
 * Emits today's local epoch day and the next one right after midnight. Re-checks at least every
 * few minutes, because delays can run late while the device sleeps.
 */
fun Clock.observeEpochDay(): Flow<Long> =
    flow {
        while (true) {
            val now = ZonedDateTime.now(this@observeEpochDay)
            emit(now.toLocalDate().toEpochDay())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(minOf(Duration.between(now, nextMidnight).toMillis() + 1, MAX_CHECK_INTERVAL_MILLIS))
        }
    }.distinctUntilChanged()

package io.github.loeswerkman.language_trainer

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** The current time in epoch milliseconds. */
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

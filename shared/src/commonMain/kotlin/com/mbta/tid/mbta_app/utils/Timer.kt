package com.mbta.tid.mbta_app.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class ClockTickHandler {
    companion object {
        private val _clockFlow = MutableSharedFlow<Instant>(replay = 0)
        var job: Job? = null

        fun getClockFlow(clock: Clock): SharedFlow<Instant> {
            if (job == null) {
                job =
                    CoroutineScope(Dispatchers.IO).launch {
                        while (true) {
                            _clockFlow.emit(clock.now())
                            delay(500)
                        }
                    }
            }
            return _clockFlow
        }
    }
}

@Composable
fun timer(updateInterval: Duration = 5.seconds, clock: Clock = koinInject()): State<Instant> {
    val timerFlow = remember {
        ClockTickHandler.getClockFlow(clock).filter {
            it.toEpochMilliseconds().seconds.inWholeSeconds % updateInterval.inWholeSeconds == 0L
        }
    }
    return timerFlow.collectAsState(initial = clock.now())
}

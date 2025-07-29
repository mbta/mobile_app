package com.mbta.tid.mbta_app.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
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
        private val _clockFlow = MutableSharedFlow<EasternTimeInstant>(replay = 0)
        var job: Job? = null

        fun getClockFlow(clock: Clock): SharedFlow<EasternTimeInstant> {
            if (job == null) {
                job =
                    CoroutineScope(Dispatchers.IO).launch {
                        while (true) {
                            _clockFlow.emit(EasternTimeInstant.now(clock))
                            delay(500)
                        }
                    }
            }
            return _clockFlow
        }
    }
}

@Composable
fun timer(
    updateInterval: Duration = 5.seconds,
    clock: Clock = koinInject(),
): State<EasternTimeInstant> {
    val timerFlow = remember {
        ClockTickHandler.getClockFlow(clock).filter {
            it.secondsHasDivisor(updateInterval.inWholeSeconds)
        }
    }
    return timerFlow.collectAsState(initial = EasternTimeInstant.now(clock))
}

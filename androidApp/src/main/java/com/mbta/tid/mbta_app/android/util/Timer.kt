package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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

class TimerViewModel(tickInterval: Duration, clock: Clock) : ViewModel() {
    val timerFlow =
        ClockTickHandler.getClockFlow(clock).filter {
            it.toEpochMilliseconds().seconds.inWholeSeconds % tickInterval.inWholeSeconds == 0L
        }
}

@Composable
fun timer(updateInterval: Duration = 5.seconds, clock: Clock = koinInject()): State<Instant> {
    val viewModel: TimerViewModel = remember { TimerViewModel(updateInterval, clock) }
    return viewModel.timerFlow.collectAsState(initial = clock.now())
}

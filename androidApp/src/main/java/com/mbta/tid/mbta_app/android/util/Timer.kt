package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class ClockTickHandler {
    companion object {
        private val _clockFlow = MutableSharedFlow<Instant>(replay = 0)
        var job: Job? = null

        fun getClockFlow(): SharedFlow<Instant> {
            if (job == null) {
                job =
                    CoroutineScope(Dispatchers.IO).launch {
                        while (true) {
                            _clockFlow.emit(Clock.System.now())
                            delay(500)
                        }
                    }
            }
            return _clockFlow
        }
    }
}

class TimerViewModel(tickInterval: Duration) : ViewModel() {
    val timerFlow =
        ClockTickHandler.getClockFlow().filter {
            it.toEpochMilliseconds().seconds.inWholeSeconds % tickInterval.inWholeSeconds == 0L
        }
}

@Composable
fun timer(updateInterval: Duration = 5.seconds): State<Instant> {
    val viewModel: TimerViewModel = remember { TimerViewModel(updateInterval) }
    return viewModel.timerFlow.collectAsState(initial = Clock.System.now())
}

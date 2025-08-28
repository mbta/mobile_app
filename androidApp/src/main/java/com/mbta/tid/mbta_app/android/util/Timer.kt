package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Clock
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
                            val now = EasternTimeInstant.now(clock)
                            _clockFlow.emit(now)
                            val untilNextSecond = 1000 - now.instant.toEpochMilliseconds() % 1000
                            delay(untilNextSecond)
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
            it.secondsHasDivisor(tickInterval.inWholeSeconds)
        }
}

@Composable
fun timer(
    updateInterval: Duration = 5.seconds,
    clock: Clock = koinInject(),
): State<EasternTimeInstant> {
    val viewModel: TimerViewModel = remember { TimerViewModel(updateInterval, clock) }
    return viewModel.timerFlow.collectAsState(initial = EasternTimeInstant.now(clock))
}

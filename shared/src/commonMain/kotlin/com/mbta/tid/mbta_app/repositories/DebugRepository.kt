package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent

public class DebugState(public val channelUpdates: Map<String, EasternTimeInstant> = emptyMap())

public abstract class IDebugRepository
internal constructor(initialState: DebugState? = null, private val clock: Clock = Clock.System) :
    KoinComponent {

    protected val flow: MutableStateFlow<DebugState?> = MutableStateFlow(initialState)
    public val state: StateFlow<DebugState?> = flow.asStateFlow()

    private val channelUpdates = mutableMapOf<String, EasternTimeInstant>()
    private val mutex = Mutex()

    public open suspend fun setChannelSuccess(topic: String) {
        mutex.withLock {
            channelUpdates[topic] = EasternTimeInstant.now(clock)
            updateState()
        }
    }

    public open suspend fun clearChannelStatus(topic: String) {
        mutex.withLock {
            channelUpdates.remove(topic)
            updateState()
        }
    }

    private fun updateState() {
        // Updates must be copied with .toMap to avoid concurrent modification exceptions in the UI
        flow.value = DebugState(channelUpdates.toMap())
    }
}

public class DebugRepository(initialState: DebugState? = null, clock: Clock = Clock.System) :
    IDebugRepository(initialState, clock), KoinComponent

public class MockDebugRepository
@DefaultArgumentInterop.Enabled
constructor(
    state: DebugState? = null,
    private val onSetChannelSuccess: ((String) -> Unit)? = null,
    private val onClearChannelStatus: ((String) -> Unit)? = null,
) : IDebugRepository(state) {

    override suspend fun setChannelSuccess(topic: String): Unit {
        onSetChannelSuccess?.invoke(topic)
    }

    override suspend fun clearChannelStatus(topic: String) {
        onClearChannelStatus?.invoke(topic)
    }

    public val mutableFlow: MutableStateFlow<DebugState?>
        get() = flow
}

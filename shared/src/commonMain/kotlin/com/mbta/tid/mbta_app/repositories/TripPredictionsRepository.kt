package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.ChannelOwner
import com.mbta.tid.mbta_app.phoenix.PredictionsForTripChannel
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.component.KoinComponent

public interface ITripPredictionsRepository {
    public fun connect(
        tripId: String,
        errorKey: ErrorKey,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit,
    )

    public var lastUpdated: EasternTimeInstant?

    public fun shouldForgetPredictions(predictionCount: Int): Boolean

    public fun disconnect()
}

internal class TripPredictionsRepository(
    socket: PhoenixSocket,
    debugRepository: IDebugRepository,
    errorBannerStateRepository: IErrorBannerStateRepository,
    ioDispatcher: CoroutineDispatcher,
) : ITripPredictionsRepository, KoinComponent {
    private val channelOwner =
        ChannelOwner<PredictionsStreamDataResponse>(
            socket,
            ioDispatcher,
            debugRepository,
            errorBannerStateRepository,
        )
    internal var channel: PhoenixChannel? by channelOwner::channel

    override var lastUpdated: EasternTimeInstant? = null

    override fun shouldForgetPredictions(predictionCount: Int) =
        (EasternTimeInstant.now() - (lastUpdated ?: EasternTimeInstant(Instant.DISTANT_FUTURE))) >
            10.minutes && predictionCount > 0

    override fun connect(
        tripId: String,
        errorKey: ErrorKey,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit,
    ) {
        channelOwner.connect(
            PredictionsForTripChannel(tripId),
            PredictionsForTripChannel::parseMessage,
            handleResult = {
                when (it) {
                    is ApiResult.Ok -> {
                        println("Received ${it.data.predictions.size} predictions")
                        lastUpdated = EasternTimeInstant.now()
                    }
                    else -> {}
                }
                onReceive(it)
            },
            errorKey = errorKey,
        )
    }

    override fun disconnect() {
        channelOwner.disconnect()
    }
}

public class MockTripPredictionsRepository
@DefaultArgumentInterop.Enabled
constructor(
    internal var onConnect: () -> Unit = {},
    internal var onDisconnect: () -> Unit = {},
    internal var response: PredictionsStreamDataResponse =
        PredictionsStreamDataResponse(emptyMap(), emptyMap(), emptyMap()),
) : ITripPredictionsRepository {

    override fun connect(
        tripId: String,
        errorKey: ErrorKey,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit,
    ) {
        onReceive(ApiResult.Ok(response))
        onConnect()
    }

    override var lastUpdated: EasternTimeInstant? = null

    override fun shouldForgetPredictions(predictionCount: Int): Boolean = false

    override fun disconnect() {
        onDisconnect()
    }
}

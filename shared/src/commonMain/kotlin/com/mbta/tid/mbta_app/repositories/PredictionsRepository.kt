package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.AsymmetricChannelOwner
import com.mbta.tid.mbta_app.phoenix.PredictionsForStopsChannel
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.component.KoinComponent

public interface IPredictionsRepository {
    public fun connect(
        stopIds: List<String>,
        errorKey: String,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit,
    )

    public var lastUpdated: EasternTimeInstant?

    public fun shouldForgetPredictions(predictionCount: Int): Boolean

    public fun disconnect()
}

internal class PredictionsRepository(
    socket: PhoenixSocket,
    debugRepository: IDebugRepository,
    errorBannerStateRepository: IErrorBannerStateRepository,
    ioDispatcher: CoroutineDispatcher,
) : IPredictionsRepository, KoinComponent {
    private val channelOwner =
        AsymmetricChannelOwner<PredictionsByStopJoinResponse, PredictionsByStopMessageResponse>(
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
        stopIds: List<String>,
        errorKey: String,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit,
    ) {
        channelOwner.connect(
            PredictionsForStopsChannel.V2(stopIds),
            parseJoinMessage = PredictionsForStopsChannel::parseV2JoinMessage,
            parseMessage = PredictionsForStopsChannel::parseV2Message,
            handleJoinResult = {
                when (it) {
                    is ApiResult.Ok -> {
                        val predictionCount =
                            it.data.predictionsByStop.values.flatMap { stop -> stop.values }.size
                        println("Received $predictionCount predictions on join")
                        lastUpdated = EasternTimeInstant.now()
                    }
                    else -> {}
                }
                onJoin(it)
            },
            handleResult = {
                when (it) {
                    is ApiResult.Ok -> lastUpdated = EasternTimeInstant.now()
                    else -> {}
                }
                onMessage(it)
            },
            errorKey = errorKey,
        )
    }

    override fun disconnect() {
        channelOwner.disconnect()
    }
}

public class MockPredictionsRepository
@DefaultArgumentInterop.Enabled
constructor(
    internal val onConnectV2: (List<String>) -> Unit = {},
    internal val onDisconnect: () -> Unit = {},
    private val connectV2Outcome: ApiResult<PredictionsByStopJoinResponse>? = null,
) : IPredictionsRepository {

    @DefaultArgumentInterop.Enabled
    public constructor(
        onConnectV2: (List<String>) -> Unit = {},
        onDisconnect: () -> Unit = {},
        // v2 response is required because that's the main one we actually use, and not including
        // a required param results in ambiguous constructor signatures
        connectV2Response: PredictionsByStopJoinResponse,
    ) : this(onConnectV2, onDisconnect, ApiResult.Ok(connectV2Response))

    override fun connect(
        stopIds: List<String>,
        errorKey: String,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit,
    ) {
        lastUpdated = EasternTimeInstant.now()
        onConnectV2(stopIds)
        if (connectV2Outcome != null) {
            onJoin(connectV2Outcome)
        }
        this.onMessage = onMessage
    }

    internal var onMessage: ((ApiResult<PredictionsByStopMessageResponse>) -> Unit)? = null

    internal fun sendMessage(message: PredictionsByStopMessageResponse) {
        lastUpdated = EasternTimeInstant.now()
        onMessage?.invoke(ApiResult.Ok(message))
    }

    override var lastUpdated: EasternTimeInstant? = null

    override fun shouldForgetPredictions(predictionCount: Int): Boolean = false

    override fun disconnect() {
        onDisconnect()
    }
}

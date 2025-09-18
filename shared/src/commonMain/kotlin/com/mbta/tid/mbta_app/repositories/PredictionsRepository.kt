package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.phoenix.ChannelOwner
import com.mbta.tid.mbta_app.phoenix.PredictionsForStopsChannel
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.koin.core.component.KoinComponent

public interface IPredictionsRepository {
    public fun connect(
        stopIds: List<String>,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit,
    )

    public fun connectV2(
        stopIds: List<String>,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit,
    )

    public var lastUpdated: EasternTimeInstant?

    public fun shouldForgetPredictions(predictionCount: Int): Boolean

    public fun disconnect()
}

internal class PredictionsRepository(socket: PhoenixSocket) :
    IPredictionsRepository, KoinComponent {
    private val channelOwner = ChannelOwner(socket)
    internal var channel: PhoenixChannel? by channelOwner::channel

    override var lastUpdated: EasternTimeInstant? = null

    override fun shouldForgetPredictions(predictionCount: Int) =
        (EasternTimeInstant.now() - (lastUpdated ?: EasternTimeInstant(Instant.DISTANT_FUTURE))) >
            10.minutes && predictionCount > 0

    override fun connect(
        stopIds: List<String>,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit,
    ) {
        channelOwner.connect(
            PredictionsForStopsChannel.V1(stopIds),
            handleMessage = { handleNewDataMessage(it, onReceive) },
            handleError = { onReceive(ApiResult.Error(message = it)) },
        )
    }

    override fun connectV2(
        stopIds: List<String>,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit,
    ) {
        channelOwner.connect(
            PredictionsForStopsChannel.V2(stopIds),
            handleJoin = { handleV2JoinMessage(it, onJoin) },
            handleMessage = { handleV2Message(it, onMessage) },
            handleError = { onMessage(ApiResult.Error(message = it)) },
        )
    }

    override fun disconnect() {
        channelOwner.disconnect()
    }

    private fun handleNewDataMessage(
        message: PhoenixMessage,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit,
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newPredictions =
                try {
                    PredictionsForStopsChannel.parseMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onReceive(ApiResult.Error(message = SocketError.FAILED_TO_PARSE))
                    return
                }
            println("Received ${newPredictions.predictions.size} predictions")
            lastUpdated = EasternTimeInstant.now()
            onReceive(ApiResult.Ok(newPredictions))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }

    private fun handleV2JoinMessage(
        message: PhoenixMessage,
        onJoin: (ApiResult<PredictionsByStopJoinResponse>) -> Unit,
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newPredictionsByStop =
                try {
                    PredictionsForStopsChannel.parseV2JoinMessage(rawPayload)
                } catch (e: IllegalArgumentException) {
                    print("ERROR $e")
                    onJoin(ApiResult.Error(message = SocketError.FAILED_TO_PARSE))
                    return
                }
            println(
                "Received ${newPredictionsByStop.predictionsByStop.values.flatMap { it.values}.size} predictions on join"
            )
            lastUpdated = EasternTimeInstant.now()
            onJoin(ApiResult.Ok(newPredictionsByStop))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }

    /**
     * Parse the phoenix message & pass to the onMessage callback
     *
     * @param message: the message to parse, expected as a PredictionsByStopMessageResponse
     * @param onMessage: the callback ot invoke on the parsed message
     */
    internal fun handleV2Message(
        message: PhoenixMessage,
        onMessage: (ApiResult<PredictionsByStopMessageResponse>) -> Unit,
    ) {
        val rawPayload: String? = message.jsonBody

        if (rawPayload != null) {
            val newPredictionsForStop =
                try {
                    PredictionsForStopsChannel.parseV2Message(rawPayload)
                } catch (e: IllegalArgumentException) {
                    onMessage(ApiResult.Error(message = SocketError.FAILED_TO_PARSE))
                    return
                }
            lastUpdated = EasternTimeInstant.now()
            onMessage(ApiResult.Ok(newPredictionsForStop))
        } else {
            println("No jsonPayload found for message ${message.body}")
        }
    }
}

public class MockPredictionsRepository
@DefaultArgumentInterop.Enabled
constructor(
    internal val onConnect: () -> Unit = {},
    internal val onConnectV2: (List<String>) -> Unit = {},
    internal val onDisconnect: () -> Unit = {},
    private val connectOutcome: ApiResult<PredictionsStreamDataResponse>? = null,
    private val connectV2Outcome: ApiResult<PredictionsByStopJoinResponse>? = null,
) : IPredictionsRepository {

    @DefaultArgumentInterop.Enabled
    public constructor(
        onConnect: () -> Unit = {},
        onConnectV2: (List<String>) -> Unit = {},
        onDisconnect: () -> Unit = {},
        connectResponse: PredictionsStreamDataResponse? = null,
        // v2 response is required because that's the main one we actually use, and not including
        // a required param results in ambiguous constructor signatures
        connectV2Response: PredictionsByStopJoinResponse,
    ) : this(
        onConnect,
        onConnectV2,
        onDisconnect,
        if (connectResponse != null) {
            ApiResult.Ok(connectResponse)
        } else {
            null
        },
        ApiResult.Ok(connectV2Response),
    )

    override fun connect(
        stopIds: List<String>,
        onReceive: (ApiResult<PredictionsStreamDataResponse>) -> Unit,
    ) {
        onConnect()
        if (connectOutcome != null) {
            onReceive(connectOutcome)
        }
    }

    override fun connectV2(
        stopIds: List<String>,
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

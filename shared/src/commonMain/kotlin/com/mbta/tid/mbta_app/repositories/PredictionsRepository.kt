package com.mbta.tid.mbta_app.repositories

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.PredictionsByStopJoinResponse
import com.mbta.tid.mbta_app.model.response.PredictionsByStopMessageResponse
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

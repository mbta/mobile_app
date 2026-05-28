package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.network.receiveAll
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ChannelOwner<MessageData : Any>(
    socket: PhoenixSocket,
    dispatcher: CoroutineDispatcher,
    errorBannerStateRepository: IErrorBannerStateRepository,
) {
    private val owner =
        AsymmetricChannelOwner<MessageData, MessageData>(
            socket,
            dispatcher,
            errorBannerStateRepository,
        )
    internal var channel: PhoenixChannel?
        get() = owner.channel
        set(channel) {
            owner.channel = channel
        }

    fun connect(
        spec: ChannelSpec,
        parseMessage: (String) -> MessageData,
        handleResult: (ApiResult<MessageData>) -> Unit,
        errorKey: String,
    ) = owner.connect(spec, parseMessage, parseMessage, handleResult, handleResult, errorKey)

    fun disconnect() = owner.disconnect()
}

internal class AsymmetricChannelOwner<JoinData : Any, MessageData : Any>(
    private val socket: PhoenixSocket,
    private val dispatcher: CoroutineDispatcher,
    private val errorBannerStateRepository: IErrorBannerStateRepository,
) {
    internal var channel: PhoenixChannel? = null
    private var lastMessageTimestamp: EasternTimeInstant? = null
    private val gracePeriod = 30.seconds
    private val shouldShowError: Boolean
        get() {
            val now = EasternTimeInstant.now()
            return lastMessageTimestamp?.let { (now - it) >= gracePeriod } ?: true
        }

    private val connectLock = Mutex()

    fun connect(
        spec: ChannelSpec,
        parseJoinMessage: (String) -> JoinData,
        parseMessage: (String) -> MessageData,
        handleJoinResult: (ApiResult<JoinData>) -> Unit,
        handleResult: (ApiResult<MessageData>) -> Unit,
        errorKey: String,
    ) {
        fun <Data : Any> parseResult(
            message: PhoenixMessage,
            parse: (String) -> Data,
        ): ApiResult<Data> {
            val rawPayload: String? = message.jsonBody

            val errorMessage =
                if (rawPayload != null) {
                    try {
                        return ApiResult.Ok(parse(rawPayload))
                    } catch (e: IllegalArgumentException) {
                        "Failed to parse ${message.subject} channel message: ${e.message}"
                    }
                } else {
                    "No jsonPayload found for ${message.subject} message ${message.body}"
                }
            return ApiResult.Error(message = "${SocketError.FAILED_TO_PARSE} - $errorMessage")
        }

        fun handleJoinResultAndBanner(result: ApiResult.Ok<JoinData>) {
            CoroutineScope(dispatcher).launch {
                errorBannerStateRepository.clearDataError(errorKey)
            }
            handleJoinResult(result)
        }

        fun handleResultAndBanner(result: ApiResult.Ok<MessageData>) {
            CoroutineScope(dispatcher).launch {
                errorBannerStateRepository.clearDataError(errorKey)
            }
            handleResult(result)
            lastMessageTimestamp = EasternTimeInstant.now()
        }

        fun handleJoinErrorAndBanner(result: ApiResult.Error<JoinData>) {
            if (shouldShowError) {
                CoroutineScope(dispatcher).launch {
                    errorBannerStateRepository.setDataError(errorKey, result.message) {
                        connect(
                            spec,
                            parseJoinMessage,
                            parseMessage,
                            handleJoinResult,
                            handleResult,
                            errorKey,
                        )
                    }
                }
            }
            handleJoinResult(result)
        }

        disconnect()
        CoroutineScope(dispatcher).launch {
            connectLock.withLock {
                val channel = socket.getChannel(spec.topic, spec.params)

                channel.onEvent(spec.updateEvent) {
                    when (val result = parseResult(it, parseMessage)) {
                        is ApiResult.Ok<MessageData> -> handleResultAndBanner(result)
                        is ApiResult.Error<MessageData> -> handleResult(result)
                    }
                }
                channel.onFailure {
                    handleResult(ApiResult.Error(message = "${SocketError.FAILURE} - $it"))
                }

                channel.onDetach { message -> println("leaving channel ${message.subject}") }
                channel
                    .attach()
                    .receiveAll(
                        onOk = { message ->
                            println("joined channel ${message.subject}")
                            when (val result = parseResult(message, parseJoinMessage)) {
                                is ApiResult.Ok<JoinData> -> handleJoinResultAndBanner(result)
                                is ApiResult.Error<JoinData> -> handleJoinErrorAndBanner(result)
                            }
                        },
                        onError = {
                            handleJoinErrorAndBanner(
                                ApiResult.Error(message = "${SocketError.RECEIVED_ERROR} - $it")
                            )
                        },
                        onTimeout = {
                            handleJoinErrorAndBanner(
                                ApiResult.Error(
                                    message = "${SocketError.TIMEOUT} - ${it.subject} ${it.body}"
                                )
                            )
                        },
                    )
                this@AsymmetricChannelOwner.channel = channel
            }
        }
    }

    fun disconnect() {
        channel?.detach()
        channel = null
    }
}

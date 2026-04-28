package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.network.receiveAll
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ChannelOwner<X : Any>(
    socket: PhoenixSocket,
    dispatcher: CoroutineDispatcher,
    errorBannerStateRepository: IErrorBannerStateRepository,
) {
    private val owner = AsymmetricChannelOwner<X, X>(socket, dispatcher, errorBannerStateRepository)
    internal var channel: PhoenixChannel?
        get() = owner.channel
        set(channel) {
            owner.channel = channel
        }

    fun connect(
        spec: ChannelSpec,
        parseMessage: (String) -> X,
        handleResult: (ApiResult<X>) -> Unit,
        errorKey: String,
    ) = owner.connect(spec, parseMessage, parseMessage, handleResult, handleResult, errorKey)

    fun disconnect() = owner.disconnect()
}

internal class AsymmetricChannelOwner<X : Any, Y : Any>(
    private val socket: PhoenixSocket,
    private val dispatcher: CoroutineDispatcher,
    private val errorBannerStateRepository: IErrorBannerStateRepository,
) {
    internal var channel: PhoenixChannel? = null
    private val connectLock = Mutex()

    fun connect(
        spec: ChannelSpec,
        parseJoinMessage: (String) -> X,
        parseMessage: (String) -> Y,
        handleJoinResult: (ApiResult<X>) -> Unit,
        handleResult: (ApiResult<Y>) -> Unit,
        errorKey: String,
    ) {
        fun <Z : Any> parseResult(message: PhoenixMessage, parse: (String) -> Z): ApiResult<Z> {
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

        fun handleJoinResultAndBanner(result: ApiResult.Ok<X>) {
            errorBannerStateRepository.clearDataError(errorKey)
            handleJoinResult(result)
        }

        fun handleResultAndBanner(result: ApiResult.Ok<Y>) {
            errorBannerStateRepository.clearDataError(errorKey)
            handleResult(result)
        }

        fun handleJoinErrorAndBanner(result: ApiResult.Error<X>) {
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
            handleJoinResult(result)
        }

        disconnect()
        CoroutineScope(dispatcher).launch {
            connectLock.withLock {
                val channel = socket.getChannel(spec.topic, spec.params)

                channel.onEvent(spec.updateEvent) {
                    when (val result = parseResult(it, parseMessage)) {
                        is ApiResult.Ok<Y> -> handleResultAndBanner(result)
                        is ApiResult.Error<Y> -> handleResult(result)
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
                                is ApiResult.Ok<X> -> handleJoinResultAndBanner(result)
                                is ApiResult.Error<X> -> handleJoinErrorAndBanner(result)
                            }
                        },
                        onError = {
                            handleJoinErrorAndBanner(
                                ApiResult.Error(message = "${SocketError.RECEIVED_ERROR} - $it")
                            )
                        },
                        onTimeout = {
                            handleJoinErrorAndBanner(
                                ApiResult.Error(message = "${SocketError.TIMEOUT} - $it")
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

package com.mbta.tid.mbta_app.phoenix

import com.mbta.tid.mbta_app.model.SocketError
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixMessage
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.network.receiveAll
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.repositories.IDebugRepository
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ChannelOwner<MessageData : Any>(
    socket: PhoenixSocket,
    dispatcher: CoroutineDispatcher,
    debugRepository: IDebugRepository,
    errorBannerStateRepository: IErrorBannerStateRepository,
) {
    internal val owner =
        AsymmetricChannelOwner<MessageData, MessageData>(
            socket,
            dispatcher,
            debugRepository,
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
        errorKey: ErrorKey,
    ) = owner.connect(spec, parseMessage, parseMessage, handleResult, handleResult, errorKey)

    fun disconnect() = owner.disconnect()
}

internal class AsymmetricChannelOwner<JoinData : Any, MessageData : Any>(
    private val socket: PhoenixSocket,
    private val dispatcher: CoroutineDispatcher,
    private val debugRepository: IDebugRepository,
    private val errorBannerStateRepository: IErrorBannerStateRepository,
) {
    internal var channel: PhoenixChannel? = null

    private val connectLock = Mutex()

    fun connect(
        spec: ChannelSpec,
        parseJoinMessage: (String) -> JoinData,
        parseMessage: (String) -> MessageData,
        handleJoinResult: (ApiResult<JoinData>) -> Unit,
        handleResult: (ApiResult<MessageData>) -> Unit,
        errorKey: ErrorKey,
    ) {
        fun <Data : Any> parseResult(
            message: PhoenixMessage,
            parse: (String) -> Data,
        ): ApiResult<Data> {
            val rawPayload: String? = message.jsonBody

            val errorMessage =
                if (rawPayload != null) {
                    try {
                        CoroutineScope(dispatcher).launch {
                            debugRepository.setChannelSuccess(spec.topic)
                        }
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
        }

        fun handleJoinErrorAndBanner(result: ApiResult.Error<JoinData>) {
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
            handleJoinResult(result)
        }

        CoroutineScope(dispatcher).launch {
            // Wait for the disconnect to finish before attempting to connect
            disconnect().join()
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
                    CoroutineScope(dispatcher).launch {
                        debugRepository.clearChannelStatus(spec.topic)
                    }
                }

                channel.onDetach { message ->
                    println("leaving channel ${message.subject}")
                    CoroutineScope(dispatcher).launch {
                        debugRepository.clearChannelStatus(spec.topic)
                    }
                }
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

    fun disconnect(): Job =
        CoroutineScope(dispatcher).launch {
            connectLock.withLock {
                channel?.detach()
                channel = null
            }
        }
}

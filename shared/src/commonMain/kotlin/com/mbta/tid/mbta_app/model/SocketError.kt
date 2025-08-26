package com.mbta.tid.mbta_app.model

internal object SocketError {
    const val FAILURE = "channel failure"
    const val RECEIVED_ERROR = "channel received error"
    const val FAILED_TO_PARSE = "channel failed to parse"
    const val TIMEOUT = "channel timed out"
}

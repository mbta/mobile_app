package com.mbta.tid.mbta_app.model.response

import kotlinx.serialization.Serializable

@Serializable data class ErrorDetails(val code: Int? = null, val message: String)

sealed class ApiResult<T : Any> {
    data class Ok<T : Any>(val data: T) : ApiResult<T>()

    data class Error<T : Any>(val error: ErrorDetails) : ApiResult<T>()
}

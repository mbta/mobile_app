package com.mbta.tid.mbta_app.model.response

import io.ktor.client.plugins.ResponseException

sealed class ApiResult<out T : Any> {
    data class Ok<T : Any>(val data: T) : ApiResult<T>()

    data class Error<T : Any>(val code: Int? = null, val message: String) : ApiResult<T>()

    @Throws(IllegalStateException::class)
    fun dataOrThrow(): T {
        check(this is Ok) { this }
        return this.data
    }

    companion object {
        inline fun <T : Any> runCatching(block: () -> T): ApiResult<T> {
            return try {
                Ok(block())
            } catch (e: ResponseException) {
                Error(code = e.response.status.value, message = e.message ?: e.toString())
            } catch (e: Throwable) {
                // HttpResponseValidator throws ResponseException on non-success, preserve the HTTP
                // status code
                val code =
                    if (e is ResponseException) {
                        e.response.status.value
                    } else {
                        null
                    }
                Error(code = code, message = e.message ?: e.toString())
            }
        }
    }
}

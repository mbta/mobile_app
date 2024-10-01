package com.mbta.tid.mbta_app.model.response

sealed class ApiResult<T : Any> {
    data class Ok<T : Any>(val data: T) : ApiResult<T>()

    data class Error<T : Any>(val code: Int? = null, val message: String) : ApiResult<T>()

    @Throws(IllegalStateException::class)
    fun dataOrThrow(): T {
        check(this is Ok)
        return this.data
    }
}

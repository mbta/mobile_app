package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository

/**
 * If `getData` returns an `ApiResultOk`, clears any preexisting data error in `errorKey` in
 * `errorBannerRepo` and calls `onSuccess`. If `getData` returns an `ApiResultError` or throws, sets
 * an error in `errorKey` in `errorBannerRepo` with the given `onRefreshAfterError`.
 */
internal suspend fun <T : Any> fetchApi(
    errorBannerRepo: IErrorBannerStateRepository,
    errorKey: String,
    getData: suspend () -> ApiResult<T>,
    onSuccess: suspend (T) -> Unit = {},
    onRefreshAfterError: () -> Unit,
) {
    val result: ApiResult<T> =
        try {
            getData()
        } catch (e: Exception) {
            ApiResult.Error(code = null, message = e.message ?: "")
        }
    when (result) {
        is ApiResult.Error -> {
            println("fetchApi error: API request $errorKey failed: $result")
            errorBannerRepo.setDataError(key = errorKey, action = onRefreshAfterError)
        }
        is ApiResult.Ok -> {
            errorBannerRepo.clearDataError(key = errorKey)
            onSuccess(result.data)
        }
    }
}

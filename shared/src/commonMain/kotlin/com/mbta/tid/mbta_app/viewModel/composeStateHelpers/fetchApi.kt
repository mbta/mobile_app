package com.mbta.tid.mbta_app.viewModel.composeStateHelpers

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * If `getData` returns an `ApiResultOk`, clears any preexisting data error in `errorKey` in
 * `errorBannerRepo` and calls `onSuccess`. If `getData` returns an `ApiResultError` or throws, sets
 * an error in `errorKey` in `errorBannerRepo` with the given `onRefreshAfterError`.
 *
 * If the calling coroutine is canceled, neither `onSuccess` nor the error banner is touched, so
 * that results of superseded requests can't overwrite newer state.
 */
internal suspend fun <T : Any> fetchApi(
    errorBannerRepo: IErrorBannerStateRepository,
    errorKey: ErrorKey,
    getData: suspend () -> ApiResult<T>,
    onSuccess: suspend (T) -> Unit = {},
    onRefreshAfterError: () -> Unit,
    onError: (ApiResult.Error<T>) -> Unit = {},
) {
    val result: ApiResult<T> =
        try {
            getData()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Error(code = null, message = e.message ?: "")
        }
    // Repositories may convert a CancellationException into an ApiResult.Error, so check again
    currentCoroutineContext().ensureActive()

    when (result) {
        is ApiResult.Error -> {
            println("fetchApi error: API request $errorKey failed: $result")
            errorBannerRepo.setDataError(
                key = errorKey,
                details = result.toString(),
                action = onRefreshAfterError,
            )
            onError(result)
        }
        is ApiResult.Ok -> {
            errorBannerRepo.clearDataError(key = errorKey)
            onSuccess(result.data)
        }
    }
}

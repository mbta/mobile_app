//
//  FetchApi.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-09-30.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared

/// If `getData` returns an `ApiResultOk`, clears any preexisting data error in `errorKey` in `errorBannerRepo` and
/// calls `onSuccess`.
/// If `getData` returns an `ApiResultError` or throws, sets an error in `errorKey` in `errorBannerRepo` with the given
/// `onRefreshAfterError`.
/// If `getData` is interrupted by task cancellation, does nothing.
func fetchApi<T>(
    _ errorBannerRepo: IErrorBannerStateRepository = RepositoryDI().errorBanner,
    errorKey: String,
    getData: () async throws -> ApiResult<T>,
    onSuccess: @MainActor (T) -> Void = { _ in },
    onRefreshAfterError: @escaping () -> Void
) async {
    var result: ApiResult<T>
    do {
        result = try await getData()
    } catch is CancellationError {
        return
    } catch {
        result = ApiResultError<T>(code: nil, message: error.localizedDescription)
    }
    switch onEnum(of: result) {
    case let .ok(result):
        errorBannerRepo.clearDataError(key: errorKey)
        await onSuccess(result.data)
    case let .error(error):
        errorBannerRepo.setDataError(key: errorKey, details: error.description(), action: onRefreshAfterError)
    }
}

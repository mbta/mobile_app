//
//  ApiResultErrorExtension.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-09-30.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared

extension ApiResultError: Error {}

/// Catches Swift errors as ApiResult.Error instances for situations where errors aren't legal to throw.
///
/// Instead of `switch onEnum(of: try await foo())`, use `switch await callApi({ try await foo() })`.
func callApi<T>(_ block: () async throws -> ApiResult<T>) async -> Skie.Shared.ApiResult.__Sealed<T> {
    do {
        return try await onEnum(of: block())
    } catch {
        return onEnum(of: ApiResultError<T>(code: nil, message: error.localizedDescription))
    }
}

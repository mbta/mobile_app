//
//  Backend.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-01-22.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

protocol BackendProtocol {
    func getGlobalData() async throws -> StopAndRoutePatternResponse
    func getNearby(latitude: Double, longitude: Double) async throws -> StopAndRoutePatternResponse
    func getSearchResults(query: String) async throws -> SearchResponse
    func getRailRouteShapes() async throws -> RouteResponse
    func predictionsStopsChannel(stopIds: [String]) async throws -> PredictionsStopsChannel
}

extension Backend: BackendProtocol {}

struct IdleBackend: BackendProtocol {
    private func hang() async -> Never {
        while true {
            try? await Task.sleep(nanoseconds: .max)
        }
    }

    func getGlobalData() async throws -> StopAndRoutePatternResponse {
        await hang()
    }

    func getNearby(latitude _: Double, longitude _: Double) async throws -> StopAndRoutePatternResponse {
        await hang()
    }

    func getRailRouteShapes() async throws -> RouteResponse {
        await hang()
    }

    func getSearchResults(query _: String) async throws -> SearchResponse {
        await hang()
    }

    func predictionsStopsChannel(stopIds _: [String]) async throws -> PredictionsStopsChannel {
        await hang()
    }
}

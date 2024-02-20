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
    func getNearby(latitude: Double, longitude: Double) async throws -> StopAndRoutePatternResponse
    func getGlobalData() async throws -> StopAndRoutePatternResponse
    func getSearchResults(query: String) async throws -> SearchResponse
    func predictionsStopsChannel(stopIds: [String]) async throws -> PredictionsStopsChannel
}

extension Backend: BackendProtocol {}

struct IdleBackend: BackendProtocol {
    private func hang() async -> Never {
        while true {
            try? await Task.sleep(nanoseconds: .max)
        }
    }

    func getNearby(latitude _: Double, longitude _: Double) async throws -> StopAndRoutePatternResponse {
        await hang()
    }
    
    func getGlobalData() async throws -> StopAndRoutePatternResponse {
        await hang()
    }

    func getSearchResults(query _: String) async throws -> SearchResponse {
        await hang()
    }

    func predictionsStopsChannel(stopIds _: [String]) async throws -> PredictionsStopsChannel {
        await hang()
    }
}

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
    func runSocket() async throws
    func getGlobalData() async throws -> StopAndRoutePatternResponse
    func getNearby(latitude: Double, longitude: Double) async throws -> StopAndRoutePatternResponse
    func getSchedule(stopIds: [String]) async throws -> ScheduleResponse
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

    func runSocket() async throws {
        await hang()
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

    func getSchedule(stopIds _: [String]) async throws -> ScheduleResponse {
        await hang()
    }

    func getSearchResults(query _: String) async throws -> SearchResponse {
        await hang()
    }

    func predictionsStopsChannel(stopIds _: [String]) async throws -> PredictionsStopsChannel {
        await hang()
    }
}

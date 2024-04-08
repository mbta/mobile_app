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
    func getGlobalData() async throws -> GlobalResponse
    func getNearby(latitude: Double, longitude: Double) async throws -> NearbyResponse
    func getSchedule(stopIds: [String]) async throws -> ScheduleResponse
    func getSearchResults(query: String) async throws -> SearchResponse
    func getMapFriendlyRailShapes() async throws -> MapFriendlyRouteResponse
}

extension Backend: BackendProtocol {}

struct IdleBackend: BackendProtocol {
    private func hang() async -> Never {
        while true {
            try? await Task.sleep(nanoseconds: .max)
        }
    }

    func getGlobalData() async throws -> GlobalResponse {
        await hang()
    }

    func getNearby(latitude _: Double, longitude _: Double) async throws -> NearbyResponse {
        await hang()
    }

    func getMapFriendlyRailShapes() async throws -> MapFriendlyRouteResponse {
        await hang()
    }

    func getSchedule(stopIds _: [String]) async throws -> ScheduleResponse {
        await hang()
    }

    func getSearchResults(query _: String) async throws -> SearchResponse {
        await hang()
    }
}

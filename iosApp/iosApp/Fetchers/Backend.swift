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
    func getNearby(latitude: Double, longitude: Double) async throws -> NearbyResponse
    func getSearchResults(query: String) async throws -> SearchResponse
}

extension Backend: BackendProtocol {}

struct IdleBackend: BackendProtocol {
    func getNearby(latitude _: Double, longitude _: Double) async throws -> NearbyResponse {
        while true {
            try await Task.sleep(nanoseconds: .max)
        }
    }

    func getSearchResults(query _: String) async throws -> SearchResponse {
        while true {
            try await Task.sleep(nanoseconds: .max)
        }
    }
}

//
//  Backend.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-01-22.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class BackendDispatcher: ObservableObject {
    let backend: any BackendProtocol

    init(backend: any BackendProtocol) {
        self.backend = backend
    }

    @MainActor func getNearby(latitude: Double, longitude: Double) async throws -> NearbyResponse {
        try await backend.getNearby(latitude: latitude, longitude: longitude)
    }

    @MainActor func getSearchResults(query: String) async throws -> SearchResponse {
        try await self.backend.getSearchResults(query: query)
    }
}

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

    func getSearchResults(query: String) async throws -> SearchResponse {
        while true {
            try await Task.sleep(nanoseconds: .max)
        }
    }
}

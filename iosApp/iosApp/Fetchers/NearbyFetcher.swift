//
//  NearbyFetcher.swift
//  iosApp
//
//  Created by Simon, Emma on 2/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class NearbyFetcher: ObservableObject {
    @Published var nearby: NearbyResponse?
    @Published var nearbyByRouteAndStop: [NearbyRoute]?
    let backend: any BackendProtocol

    init(backend: any BackendProtocol) {
        self.backend = backend
    }

    @MainActor func getNearby(latitude: Double, longitude: Double) async throws {
        let response = try await backend.getNearby(
            latitude: latitude,
            longitude: longitude
        )
        nearby = response
        nearbyByRouteAndStop = nearby!.byRouteAndStop()
    }
}

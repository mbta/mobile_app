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
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    @Published var nearby: StopAndRoutePatternResponse?
    @Published var nearbyByRouteAndStop: NearbyStaticData?
    let backend: any BackendProtocol

    init(backend: any BackendProtocol) {
        self.backend = backend
    }

    @MainActor func getNearby(latitude: Double, longitude: Double) async throws {
        self.latitude = latitude
        self.longitude = longitude
        let response = try await backend.getNearby(
            latitude: latitude,
            longitude: longitude
        )
        nearby = response
        nearbyByRouteAndStop = NearbyStaticData(response: response)
    }

    func withRealtimeInfo(
        predictions: PredictionsStreamDataResponse?,
        filterAtTime: Instant
    ) -> [StopAssociatedRoute]? {
        nearbyByRouteAndStop?.withRealtimeInfo(
            sortByDistanceFrom: .init(longitude: longitude, latitude: latitude),
            predictions: predictions,
            filterAtTime: filterAtTime
        )
    }
}

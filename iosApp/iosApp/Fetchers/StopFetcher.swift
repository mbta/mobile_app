//
//  StopFetcher.swift
//  iosApp
//
//  Created by Simon, Emma on 2/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class StopFetcher: ObservableObject {
    var response: StopAndRoutePatternResponse?
    @Published var stops: [Stop]
    @Published var byRouteAndStop: [StopAssociatedRoute]
    let backend: any BackendProtocol

    init(backend: any BackendProtocol, stops: [Stop] = [], byRouteAndStop: [StopAssociatedRoute] = []) {
        self.backend = backend
        self.stops = stops
        self.byRouteAndStop = byRouteAndStop
    }

    @MainActor func getAllStops() async throws {
        do {
            let response = try await backend.getAllStops()
            self.response = response
            stops = response.stops
            byRouteAndStop = response.byRouteAndStop()
        } catch {
            stops = []
            byRouteAndStop = []
            print("Failed to load stops: \(error)")
        }
    }
}

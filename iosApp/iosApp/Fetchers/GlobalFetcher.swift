//
//  GlobalFetcher.swift
//  iosApp
//
//  Created by Simon, Emma on 2/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class GlobalFetcher: ObservableObject {
    var response: StopAndRoutePatternResponse?
    @Published var stops: [Stop]
    @Published var routes: [String: Route]
    let backend: any BackendProtocol

    init(backend: any BackendProtocol, stops: [Stop] = [], routes: [String: Route] = [:]) {
        self.backend = backend
        self.stops = stops
        self.routes = routes
    }

    @MainActor func getGlobalData() async throws {
        do {
            let response = try await backend.getGlobalData()
            self.response = response
            stops = response.stops
            routes = response.routes
        } catch {
            print("Failed to load global data: \(error)")
        }
    }
}

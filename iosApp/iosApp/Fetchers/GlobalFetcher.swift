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
    var response: GlobalResponse?
    @Published var stops: [String: Stop]
    @Published var routes: [String: Route]
    let backend: any BackendProtocol

    init(backend: any BackendProtocol, stops: [String: Stop] = [:], routes: [String: Route] = [:]) {
        self.backend = backend
        self.stops = stops
        self.routes = routes
    }

    @MainActor func getGlobalData() async throws {
        do {
            print("FETCHING")
            let response = try await backend.getGlobalData()
            print("FETCHED")
            self.response = response
            stops = response.stops
            print("STOPS \(stops.count)")
            routes = response.routes
        } catch {
            print("Failed to load global data: \(error)")
        }
    }
}

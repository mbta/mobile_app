//
//  RailRouteShapeFetcher.swift
//  iosApp
//
//  Created by Simon, Emma on 2/23/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class RailRouteShapeFetcher: ObservableObject {
    var response: RouteResponse?
    @Published var routes: [Route] = []
    let backend: any BackendProtocol

    init(backend: any BackendProtocol) {
        self.backend = backend
    }

    @MainActor func getRailRouteShapes() async throws {
        response = try await backend.getRailRouteShapes()
        routes = response!.routes
    }
}

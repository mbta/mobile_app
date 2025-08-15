//
//  LineOrRouteExtension.swift
//  iosApp
//
//  Created by Melody Horn on 5/7/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

extension RouteCardData.LineOrRoute {
    static func route(_ route: Route) -> RouteCardData.LineOrRouteRoute {
        RouteCardData.LineOrRouteRoute(route: route)
    }

    static func line(_ line: Line, _ routes: Set<Route>) -> RouteCardData.LineOrRouteLine {
        RouteCardData.LineOrRouteLine(line: line, routes: routes)
    }

    var labelWithModeIfBus: String {
        type == .bus
            ? String(format: NSLocalizedString(
                "%1$@ bus",
                comment: "Bus route name label, with the value being the route number, ex. \"1 bus\", \"66 bus\""
            ), name) : name
    }
}

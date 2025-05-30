//
//  LineOrRouteExtension.swift
//  iosApp
//
//  Created by Melody Horn on 5/7/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared

extension RouteCardData.LineOrRoute {
    static func route(_ route: Route) -> RouteCardData.LineOrRouteRoute {
        RouteCardData.LineOrRouteRoute(route: route)
    }

    static func line(_ line: Line, _ routes: Set<Route>) -> RouteCardData.LineOrRouteLine {
        RouteCardData.LineOrRouteLine(line: line, routes: routes)
    }
}

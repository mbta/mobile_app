//
//  LineOrRouteExtension.swift
//  iosApp
//
//  Created by Melody Horn on 5/7/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

typealias LineModel = Line
typealias RouteModel = Route

extension LineOrRoute {
    static func route(_ route: RouteModel) -> LineOrRoute.Route {
        LineOrRoute.Route(route: route)
    }

    static func line(_ line: LineModel, _ routes: Set<RouteModel>) -> LineOrRoute.Line {
        LineOrRoute.Line(line: line, routes: routes)
    }

    var labelWithModeIfBus: String {
        type == .bus
            ? String(format: NSLocalizedString(
                "%1$@ bus",
                comment: "Bus route name label, with the value being the route number, ex. \"1 bus\", \"66 bus\""
            ), name) : name
    }
}

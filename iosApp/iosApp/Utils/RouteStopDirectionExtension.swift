//
//  RouteStopDirectionExtension.swift
//  iosApp
//
//  Created by esimon on 8/4/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteStopDirectionLabels {
    let route: String
    let stop: String
    let direction: String
}

extension RouteStopDirection {
    func getLabels(_ global: GlobalResponse?) -> RouteStopDirectionLabels? {
        guard let global,
              let lineOrRoute = global.getLineOrRoute(lineOrRouteId: route),
              let stop = global.getStop(stopId: stop)
        else { return nil }

        let routeLabel = lineOrRoute.type == .bus
            ? NSLocalizedString(
                "\(lineOrRoute.name) bus",
                comment: "Bus route name label, with the value being the route number, ex. \"1 bus\", \"66 bus\""
            ) : lineOrRoute.name
        let directionLabel = DirectionLabel.directionNameFormatted(.init(
            directionId: direction,
            route: lineOrRoute.sortRoute
        ))

        return .init(route: routeLabel, stop: stop.name, direction: directionLabel)
    }
}

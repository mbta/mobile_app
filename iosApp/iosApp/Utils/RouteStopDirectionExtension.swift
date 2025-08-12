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

        let directionLabel = DirectionLabel.directionNameFormatted(.init(
            directionId: direction,
            route: lineOrRoute.sortRoute
        ))

        return .init(route: lineOrRoute.label, stop: stop.name, direction: directionLabel)
    }
}

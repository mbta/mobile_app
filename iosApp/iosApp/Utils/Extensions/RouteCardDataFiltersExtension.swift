//
//  RouteCardDataFiltersExtension.swift
//  iosApp
//
//  Created by esimon on 9/8/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared

extension [RouteCardData] {
    var servedRouteFilters: [StopDetailsFilterPills.FilterBy] {
        map { routeCardData in
            switch onEnum(of: routeCardData.lineOrRoute) {
            case let .line(line): .line(line.line)
            case let .route(route): .route(route.route)
            }
        }
    }
}

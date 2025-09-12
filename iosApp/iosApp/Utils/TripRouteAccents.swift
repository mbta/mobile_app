//
//  TripRouteAccents.swift
//  iosApp
//
//  Created by esimon on 9/2/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

// A subset of route attributes only for displaying as UI accents,
// this is split out to allow defaults for when a route may not exist
struct TripRouteAccents: Hashable {
    let color: Color
    let textColor: Color
    let type: RouteType

    init(color: Color = .halo, textColor: Color = .text, type: RouteType = .bus) {
        self.color = color
        self.textColor = textColor
        self.type = type
    }

    init(route: Route) {
        color = route.uiColor
        textColor = route.uiTextColor
        type = route.type
    }
}

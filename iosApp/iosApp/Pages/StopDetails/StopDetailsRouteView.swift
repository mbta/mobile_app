//
//  StopDetailsRouteView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct StopDetailsRouteView: View {
    let patternsByStop: PatternsByStop
    let now: Instant
    let pushNavEntry: (SheetNavigationStackEntry) -> Void

    var body: some View {
        // TODO: pull in pin
        RouteSection(route: patternsByStop.route, pinned: false, onPin: { _ in

        }) {
            StopDeparturesSummaryList(patternsByStop: patternsByStop, now: now, pushNavEntry: pushNavEntry)
        }
    }
}

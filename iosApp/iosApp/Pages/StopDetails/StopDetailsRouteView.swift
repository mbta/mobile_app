//
//  StopDetailsRouteView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

struct StopDetailsRouteView: View {
    var analytics: Analytics = AnalyticsProvider.shared
    let patternsByStop: PatternsByStop
    let now: Instant
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let pinned: Bool
    let onPin: (String) -> Void

    var body: some View {
        if let line = patternsByStop.line {
            LineCard(line: line, routes: patternsByStop.routes, pinned: pinned, onPin: onPin) {
                StopDeparturesSummaryList(
                    patternsByStop: patternsByStop,
                    condenseHeadsignPredictions: patternsByStop.routes.count > 1,
                    now: now,
                    context: .stopDetailsUnfiltered,
                    pushNavEntry: pushNavEntry,
                    analytics: analytics,
                    pinned: pinned
                )
            }
        } else if let route = patternsByStop.routes.first {
            RouteCard(route: route, pinned: pinned, onPin: onPin) {
                StopDeparturesSummaryList(
                    patternsByStop: patternsByStop,
                    condenseHeadsignPredictions: false,
                    now: now,
                    context: .stopDetailsUnfiltered,
                    pushNavEntry: pushNavEntry,
                    analytics: analytics,
                    pinned: pinned
                )
            }
        } else {
            EmptyView()
        }
    }
}

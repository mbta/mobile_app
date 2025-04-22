//
//  NearbyRouteView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct NearbyRouteView: View {
    let nearbyRoute: StopsAssociated.WithRoute
    let pinned: Bool
    let onPin: (String) -> Void
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let now: Instant
    let showStationAccessibility: Bool

    var body: some View {
        LegacyRouteCard(route: nearbyRoute.route, pinned: pinned, onPin: onPin) {
            ForEach(Array(nearbyRoute.patternsByStop.enumerated()), id: \.element.stop.id) { index, patternsAtStop in
                VStack(spacing: 0) {
                    NearbyStopView(
                        patternsAtStop: patternsAtStop,
                        showStationAccessibility: showStationAccessibility,
                        now: now,
                        pushNavEntry: pushNavEntry,
                        pinned: pinned
                    )
                    if index < nearbyRoute.patternsByStop.count - 1 {
                        Divider().background(Color.halo)
                    }
                }
            }
        }
    }
}

//
//  NearbyRouteView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct NearbyRouteView: View {
    let nearbyTransit: StopsAssociated
    let pinned: Bool
    let onPin: (String) -> Void
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let now: Instant

    var body: some View {
        switch onEnum(of: nearbyTransit) {
        case let .withRoute(nearbyRoute):
            RouteSection(route: nearbyRoute.route, pinned: pinned, onPin: onPin) {
                ForEach(Array(nearbyRoute.patternsByStop.enumerated()),
                        id: \.element.stop.id) { index, patternsAtStop in
                    VStack(spacing: 0) {
                        NearbyStopView(patternsAtStop: patternsAtStop, pushNavEntry: pushNavEntry, now: now)
                        if index < nearbyRoute.patternsByStop.count - 1 {
                            Divider().background(Color.halo)
                        }
                    }
                }
            }
        default:
            EmptyView()
        }
    }
}

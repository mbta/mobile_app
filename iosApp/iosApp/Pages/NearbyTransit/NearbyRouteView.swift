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
    let nearbyRoute: StopAssociatedRoute
    let now: Instant

    var body: some View {
        RoutePillSection(route: nearbyRoute.route) {
            ForEach(nearbyRoute.patternsByStop, id: \.stop.id) { patternsAtStop in
                NearbyStopView(patternsAtStop: patternsAtStop, now: now)
            }
        }
    }
}

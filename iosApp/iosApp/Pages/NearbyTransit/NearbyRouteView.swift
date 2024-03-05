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
        Section {
            ForEach(nearbyRoute.patternsByStop, id: \.stop.id) { patternsAtStop in
                NearbyStopView(patternsAtStop: patternsAtStop, now: now)
            }
        }
        header: {
            RoutePill(route: nearbyRoute.route).padding(.leading, -20)
        }
    }
}

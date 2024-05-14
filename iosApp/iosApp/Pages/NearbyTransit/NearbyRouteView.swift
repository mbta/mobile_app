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
    let pinned: Bool
    let onPin: (String) -> Void

    var body: some View {
        NearbyTransitSection(route: nearbyRoute.route, pinned: pinned, onPin: onPin) {
            ForEach(Array(nearbyRoute.patternsByStop.enumerated()), id: \.element.stop.id) { index, patternsAtStop in
                VStack(spacing: 0) {
                    NearbyStopView(patternsAtStop: patternsAtStop)
                    if index < nearbyRoute.patternsByStop.count - 1 {
                        Divider().background(Color.halo)
                    }
                }
            }
        }
    }
}

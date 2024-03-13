//
//  NearbyStopView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct NearbyStopView: View {
    let patternsAtStop: PatternsByStop
    let now: Instant

    var body: some View {
        VStack(alignment: .leading) {
            Text(patternsAtStop.stop.name).fontWeight(.bold)

            VStack(alignment: .leading) {
                ForEach(patternsAtStop.patternsByHeadsign, id: \.headsign) { patternsByHeadsign in
                    NearbyStopRoutePatternView(
                        headsign: patternsByHeadsign.headsign,
                        predictions: .from(upcomingTrips: patternsByHeadsign.upcomingTrips, now: now)
                    )
                }
            }
        }
    }
}

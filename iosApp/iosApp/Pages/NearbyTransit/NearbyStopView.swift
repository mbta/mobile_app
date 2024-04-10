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
        Text(patternsAtStop.stop.name).fontWeight(.bold)

        ForEach(patternsAtStop.patternsByHeadsign, id: \.headsign) { patternsByHeadsign in
            NavigationLink(value: SheetNavigationStackEntry.stopDetails(
                patternsAtStop.stop,
                .init(routeId: patternsAtStop.route.id, directionId: patternsByHeadsign.directionId())
            )) {
                NearbyStopRoutePatternView(
                    headsign: patternsByHeadsign.headsign,
                    predictions: patternsByHeadsign.format(now: now)
                )
            }
        }
    }
}

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

    var body: some View {
        RoutePillSection(route: patternsByStop.route) {
            ForEach(patternsByStop.patternsByHeadsign, id: \.headsign) { patternsByHeadsign in
                NearbyStopRoutePatternView(
                    headsign: patternsByHeadsign.headsign,
                    predictions: patternsByHeadsign.format(now: now)
                )
            }
        }
    }
}

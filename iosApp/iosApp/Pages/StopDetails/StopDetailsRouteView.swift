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
    var analytics: StopDetailsAnalytics = AnalyticsProvider()
    let patternsByStop: PatternsByStop
    let now: Instant
    @Binding var filter: StopDetailsFilter?
    let pushNavEntry: (SheetNavigationStackEntry) -> Void

    var body: some View {
        RoutePillSection(route: patternsByStop.route) {
            ForEach(patternsByStop.patternsByHeadsign, id: \.headsign) { patternsByHeadsign in
                let navTarget: SheetNavigationStackEntry = .stopDetails(patternsByStop.stop,
                                                                        .init(
                                                                            routeId: patternsByHeadsign.route.id,
                                                                            directionId: patternsByHeadsign
                                                                                .directionId()
                                                                        ))
                SheetNavigationLink(
                    value: navTarget,
                    action: { entry in
                        pushNavEntry(entry)
                        analytics.tappedDepartureRow(routeId: patternsByStop.route.id, stopId: patternsByStop.stop.id)
                    }
                ) {
                    HeadsignRowView(
                        headsign: patternsByHeadsign.headsign,
                        predictions: patternsByHeadsign.format(now: now),
                        routeType: patternsByHeadsign.route.type
                    )
                }
                .listRowBackground(Color.fill3)
            }
        }
    }
}

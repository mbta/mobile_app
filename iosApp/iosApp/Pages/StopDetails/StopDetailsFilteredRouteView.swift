//
//  StopDetailsFilteredRouteView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-05.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct StopDetailsFilteredRouteView: View {
    let patternsByStop: PatternsByStop
    let now: Instant
    @Binding var filter: StopDetailsFilter?

    let tripData: [(PatternsByHeadsign, PatternsByHeadsign.Format)]

    init(patternsByStop: PatternsByStop, now: Instant, filter: Binding<StopDetailsFilter?>) {
        self.patternsByStop = patternsByStop
        self.now = now
        _filter = filter

        let expectedDirection: Int32? = filter.wrappedValue?.directionId
        let trips: [PatternsByHeadsign] = patternsByStop.splitPerTrip()
        let tripsFormatted: [(PatternsByHeadsign, PatternsByHeadsign.Format)] = trips.map { ($0, $0.format(now: now)) }
        tripData = tripsFormatted.filter {
            let (patternsByHeadsign, formatted) = $0
            let trip: UpcomingTrip? = patternsByHeadsign.upcomingTrips?.first
            let tripDirectionId: Int32? = trip?.trip.directionId
            let directionIdMatches = tripDirectionId == expectedDirection
            let willDisplay = formatted is PatternsByHeadsign.FormatSome
            return directionIdMatches && willDisplay
        }
    }

    var body: some View {
        Button(action: { filter = nil }, label: { Text("Clear Filter") })
        List {
            RoutePillSection(route: patternsByStop.route) {
                ForEach(tripData, id: \.0.upcomingTrips?.first?.trip.id) { patternsByHeadsign, formatted in
                    NearbyStopRoutePatternView(
                        headsign: patternsByHeadsign.headsign,
                        predictions: formatted
                    )
                }
            }
        }
    }
}

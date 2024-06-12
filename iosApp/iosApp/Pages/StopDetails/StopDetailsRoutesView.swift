//
//  StopDetailsRoutesView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct StopDetailsRoutesView: View {
    let departures: StopDetailsDepartures
    let now: Instant
    @Binding var filter: StopDetailsFilter?
    let pushNavEntry: (SheetNavigationStackEntry) -> Void

    var body: some View {
        if filter != nil {
            StopDetailsFilteredRouteView(departures: departures, now: now, filter: $filter, pushNavEntry: pushNavEntry)
        } else {
            List(departures.routes, id: \.route.id) { patternsByStop in
                StopDetailsRouteView(patternsByStop: patternsByStop, now: now, filter: $filter)
            }
        }
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    let route1 = objects.route { route in
        route.color = "00843D"
        route.longName = "Green Line B"
        route.textColor = "FFFFFF"
        route.type = .lightRail
    }
    let route2 = objects.route { route in
        route.color = "FFC72C"
        route.shortName = "57"
        route.textColor = "000000"
        route.type = .bus
    }
    let stop = objects.stop { _ in }
    let trip1 = objects.trip { _ in }
    let prediction1 = objects.prediction { prediction in
        prediction.trip = trip1
        prediction.departureTime = (Date.now + 5 * 60).toKotlinInstant()
    }
    let trip2 = objects.trip { _ in }
    let schedule2 = objects.schedule { schedule in
        schedule.trip = trip2
        schedule.departureTime = (Date.now + 10 * 60).toKotlinInstant()
    }
    let trip3 = objects.trip { _ in }
    let prediction2 = objects.prediction { prediction in
        prediction.trip = trip3
        prediction.departureTime = (Date.now + 8 * 60).toKotlinInstant()
    }

    return StopDetailsRoutesView(departures: .init(routes: [
        .init(route: route1, stop: stop, patternsByHeadsign: [
            .init(route: route1, headsign: "A", patterns: [],
                  upcomingTrips: [.init(trip: trip1, prediction: prediction1)],
                  alertsHere: nil),
        ]),
        .init(route: route2, stop: stop, patternsByHeadsign: [
            .init(route: route2, headsign: "B", patterns: [],
                  upcomingTrips: [.init(trip: trip3, prediction: prediction2)],
                  alertsHere: nil),
            .init(route: route2, headsign: "C", patterns: [],
                  upcomingTrips: [.init(trip: trip2, schedule: schedule2)],
                  alertsHere: nil),
        ]),
    ]), now: Date.now.toKotlinInstant(), filter: .constant(nil), pushNavEntry: { _ in })
}

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
    let global: GlobalResponse?
    let now: Instant
    var filter: StopDetailsFilter?
    var setFilter: (StopDetailsFilter?) -> Void
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let pinRoute: (String) -> Void
    var pinnedRoutes: Set<String> = []

    var body: some View {
        if let filter {
            StopDetailsFilteredRouteView(
                departures: departures,
                global: global,
                now: now,
                filter: filter,
                setFilter: setFilter,
                pushNavEntry: pushNavEntry,
                pinned: pinnedRoutes.contains(filter.routeId)
            )
        } else {
            ZStack {
                Color.fill1.ignoresSafeArea(.all)
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(departures.routes, id: \.routeIdentifier) { patternsByStop in
                            StopDetailsRouteView(
                                patternsByStop: patternsByStop,
                                now: now,
                                pushNavEntry: pushNavEntry,
                                pinned: pinnedRoutes.contains(patternsByStop.routeIdentifier),
                                onPin: pinRoute
                            )
                        }
                    }.padding(.top, 16)
                }
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
    let trip4 = objects.trip { _ in }
    let schedule3 = objects.schedule { schedule in
        schedule.trip = trip4
        schedule.departureTime = (Date.now + 10 * 60).toKotlinInstant()
    }
    let prediction3 = objects.prediction { prediction in
        prediction.trip = trip4
        prediction.departureTime = nil
        prediction.arrivalTime = nil
        prediction.scheduleRelationship = .cancelled
    }

    return StopDetailsRoutesView(departures: .init(routes: [
        .init(route: route1, stop: stop, patterns: [
            .ByHeadsign(
                route: route1,
                headsign: "A",
                line: nil,
                patterns: [],
                upcomingTrips: [.init(trip: trip1, prediction: prediction1)]
            ),
        ], elevatorAlerts: []),
        .init(route: route2, stop: stop, patterns: [
            .ByHeadsign(
                route: route2,
                headsign: "B",
                line: nil,
                patterns: [],
                upcomingTrips: [.init(trip: trip3, prediction: prediction2)]
            ),
            .ByHeadsign(
                route: route2,
                headsign: "C",
                line: nil,
                patterns: [],
                upcomingTrips: [.init(trip: trip2, schedule: schedule2)]
            ),
            .ByHeadsign(
                route: route2,
                headsign: "D",
                line: nil,
                patterns: [],
                upcomingTrips: [
                    .init(trip: trip4, schedule: schedule3, prediction: prediction3),
                ]
            ),
        ], elevatorAlerts: []),
    ]), global: nil, now: Date.now.toKotlinInstant(), filter: nil, setFilter: { _ in }, pushNavEntry: { _ in },
    pinRoute: { routeId in print("Pinned route \(routeId)") }).font(Typography.body)
}

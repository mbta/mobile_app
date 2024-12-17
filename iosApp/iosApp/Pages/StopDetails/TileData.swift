//
//  TileData.swift
//  iosApp
//
//  Created by esimon on 11/27/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared

struct TileData {
    let route: Route
    let headsign: String
    let formatted: RealtimePatterns.Format
    let upcoming: UpcomingTrip?
    let navigationTarget: SheetNavigationStackEntry?

    init(
        route: Route,
        headsign: String,
        formatted: RealtimePatterns.Format,
        upcoming: UpcomingTrip? = nil,
        navigationTarget: SheetNavigationStackEntry? = nil
    ) {
        self.route = route
        self.headsign = headsign
        self.formatted = formatted
        self.upcoming = upcoming
        self.navigationTarget = navigationTarget
    }

    init?(upcoming: UpcomingTrip, route: Route, stopId: String, now: Instant) {
        let formatted = if let formattedUpcomingTrip = RealtimePatterns.companion.formatUpcomingTrip(
            now: now,
            upcomingTrip: upcoming,
            routeType: route.type,
            context: .stopDetailsFiltered
        ) {
            RealtimePatterns.FormatSome(trips: [formattedUpcomingTrip], secondaryAlert: nil)
        } else {
            RealtimePatterns.FormatNone(secondaryAlert: nil)
        }

        if !(formatted is RealtimePatterns.FormatSome) {
            return nil
        }

        self.route = route
        headsign = upcoming.trip.headsign
        self.formatted = formatted
        self.upcoming = upcoming

        if let vehicleId = upcoming.prediction?.vehicleId, let stopSequence = upcoming.stopSequence {
            navigationTarget = .tripDetails(
                tripId: upcoming.trip.id,
                vehicleId: vehicleId,
                target: .init(stopId: stopId, stopSequence: stopSequence.intValue),
                routeId: upcoming.trip.routeId,
                directionId: upcoming.trip.directionId
            )
        } else {
            navigationTarget = nil
        }
    }
}

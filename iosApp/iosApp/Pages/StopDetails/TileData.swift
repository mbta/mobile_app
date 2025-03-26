//
//  TileData.swift
//  iosApp
//
//  Created by esimon on 11/27/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared

struct TileData: Identifiable {
    let id: String
    let route: Route
    let headsign: String
    let formatted: UpcomingFormat
    let upcoming: UpcomingTrip?

    init(
        route: Route,
        headsign: String,
        formatted: UpcomingFormat,
        upcoming: UpcomingTrip? = nil
    ) {
        self.route = route
        self.headsign = headsign
        self.formatted = formatted
        self.upcoming = upcoming
        id = upcoming?.trip.id ?? UUID().uuidString
    }

    init?(upcoming: UpcomingTrip, route: Route, now: Instant) {
        let formatted = if let formattedUpcomingTrip = RealtimePatterns.companion.formatUpcomingTrip(
            now: now,
            upcomingTrip: upcoming,
            routeType: route.type,
            context: .stopDetailsFiltered
        ) {
            UpcomingFormat.Some(trips: [formattedUpcomingTrip], secondaryAlert: nil)
        } else {
            UpcomingFormat.NoTrips(
                noTripsFormat: UpcomingFormat.NoTripsFormatPredictionsUnavailable()
            )
        }

        if !(formatted is UpcomingFormat.Some) {
            return nil
        }

        self.route = route
        headsign = upcoming.trip.headsign
        self.formatted = formatted
        self.upcoming = upcoming
        id = upcoming.trip.id
    }
}

//
//  StopDetailsFilteredRouteViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-04-09.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsFilteredRouteViewTests: XCTestCase {
    private func testData() -> (departures: StopDetailsDepartures, routeId: String, now: Instant) {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let patternNorth = objects.routePattern(route: route) { pattern in
            pattern.directionId = 0
            pattern.representativeTrip { $0.headsign = "North" }
        }
        let patternSouth = objects.routePattern(route: route) { pattern in
            pattern.directionId = 1
            pattern.representativeTrip { $0.headsign = "South" }
        }

        let now = Date.now

        let tripNorth = objects.trip(routePattern: patternNorth)
        let predictionNorth = objects.prediction {
            $0.trip = tripNorth
            $0.departureTime = now.toKotlinInstant()
        }
        let tripSouth = objects.trip(routePattern: patternSouth)
        let predictionSouth = objects.prediction {
            $0.trip = tripSouth
            $0.departureTime = now.toKotlinInstant()
        }

        let patternsByStop = PatternsByStop(route: route, stop: stop, patternsByHeadsign: [
            .init(route: route, headsign: "North", patterns: [patternNorth], upcomingTrips: [objects.upcomingTrip(prediction: predictionNorth)], alertsHere: nil),
            .init(route: route, headsign: "South", patterns: [patternSouth], upcomingTrips: [objects.upcomingTrip(prediction: predictionSouth)], alertsHere: nil),
        ], directions: [])

        let departures = StopDetailsDepartures(routes: [patternsByStop])

        return (departures: departures, routeId: route.id, now: now.toKotlinInstant())
    }

    func testAppliesFilter() throws {
        let (departures: departures, routeId: routeId, now: now) = testData()

        let sut = StopDetailsFilteredRouteView(
            departures: departures,
            now: now,
            filter: .constant(.init(routeId: routeId, directionId: 0))
        )

        XCTAssertNotNil(try sut.inspect().find(text: "North"))
        XCTAssertNil(try? sut.inspect().find(text: "South"))
    }
}

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
    private func testData() -> (
        departures: StopDetailsDepartures,
        routeId: String,
        tripNorthId: String,
        stopId: String,
        vehicleNorthId: String,
        stopSequence: Int,
        now: Instant
    ) {
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
        let stopSequence = 92

        let tripNorth = objects.trip(routePattern: patternNorth)
        let vehicleNorth = objects.vehicle {
            $0.currentStatus = .stoppedAt
        }
        let predictionNorth = objects.prediction {
            $0.trip = tripNorth
            $0.vehicleId = vehicleNorth.id
            $0.departureTime = now.toKotlinInstant()
            $0.stopSequence = KotlinInt(int: Int32(stopSequence))
        }
        let tripSouth = objects.trip(routePattern: patternSouth)
        let predictionSouth = objects.prediction {
            $0.trip = tripSouth
            $0.departureTime = now.toKotlinInstant()
            $0.stopSequence = KotlinInt(int: Int32(stopSequence))
        }

        let patternsByStop = PatternsByStop(route: route, stop: stop, patternsByHeadsign: [
            .init(route: route, headsign: "North", patterns: [patternNorth], upcomingTrips: [objects.upcomingTrip(prediction: predictionNorth)], alertsHere: nil),
            .init(route: route, headsign: "South", patterns: [patternSouth], upcomingTrips: [objects.upcomingTrip(prediction: predictionSouth)], alertsHere: nil),
        ])

        let departures = StopDetailsDepartures(routes: [patternsByStop])

        return (
            departures: departures,
            routeId: route.id,
            tripNorthId: tripNorth.id,
            stopId: stop.id,
            vehicleNorthId: vehicleNorth.id,
            stopSequence: stopSequence,
            now: now.toKotlinInstant()
        )
    }

    func testAppliesFilter() throws {
        let data = testData()

        let sut = StopDetailsFilteredRouteView(
            departures: data.departures,
            now: data.now,
            filter: .constant(.init(routeId: data.routeId, directionId: 0))
        )

        XCTAssertNotNil(try sut.inspect().find(text: "North"))
        XCTAssertNil(try? sut.inspect().find(text: "South"))
    }

    func testLinks() throws {
        let data = testData()

        let sut = StopDetailsFilteredRouteView(
            departures: data.departures,
            now: data.now,
            filter: .constant(.init(routeId: data.routeId, directionId: 0))
        )

        let expected = SheetNavigationStackEntry.tripDetails(
            tripId: data.tripNorthId,
            vehicleId: data.vehicleNorthId,
            target: .init(stopId: data.stopId, stopSequence: data.stopSequence)
        )
        XCTAssertEqual(try sut.inspect().find(navigationLink: "North").value(), expected)
    }
}

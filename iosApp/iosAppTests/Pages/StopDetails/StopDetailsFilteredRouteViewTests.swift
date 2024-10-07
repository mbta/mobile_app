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
    private struct TestData {
        let departures: StopDetailsDepartures
        let routeId: String
        let lineId: String
        let tripNorthId: String
        let stopId: String
        let vehicleNorthId: String
        let stopSequence: Int
        let now: Instant
    }

    private func testData() -> TestData {
        let objects = ObjectCollectionBuilder()
        let line = objects.line()
        let route = objects.route()
        let lineRoute1 = objects.route()
        let lineRoute2 = objects.route()
        let lineRoute3 = objects.route()
        let stop = objects.stop { _ in }

        let patternNorth = objects.routePattern(route: route) { pattern in
            pattern.directionId = 0
            pattern.representativeTrip {
                $0.headsign = "North"
                $0.routePatternId = "test-north"
            }
        }
        let patternSouth = objects.routePattern(route: route) { pattern in
            pattern.directionId = 1
            pattern.representativeTrip {
                $0.headsign = "South"
                $0.routePatternId = "test-south"
            }
        }
        let linePatternTrunk1 = objects.routePattern(route: lineRoute1) { pattern in
            pattern.directionId = 0
            pattern.representativeTrip { $0.headsign = "Trunk 1" }
        }
        let linePatternTrunk2 = objects.routePattern(route: lineRoute2) { pattern in
            pattern.directionId = 0
            pattern.representativeTrip { $0.headsign = "Trunk 2" }
        }
        let linePatternBranch = objects.routePattern(route: lineRoute3) { pattern in
            pattern.directionId = 0
            pattern.representativeTrip {
                $0.headsign = "Branch"
                $0.routePatternId = "test-branch"
            }
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
            $0.stopSequence = Int32(stopSequence)
        }
        let tripSouth = objects.trip(routePattern: patternSouth)
        let predictionSouth = objects.prediction {
            $0.trip = tripSouth
            $0.departureTime = now.toKotlinInstant()
            $0.stopSequence = Int32(stopSequence)
        }

        let lineTripTrunk1 = objects.trip(routePattern: linePatternTrunk1)
        let linePredictionTrunk1 = objects.prediction {
            $0.trip = lineTripTrunk1
            $0.departureTime = now.toKotlinInstant()
            $0.stopSequence = Int32(stopSequence)
        }
        let lineTripTrunk2 = objects.trip(routePattern: linePatternTrunk2)
        let linePredictionTrunk2 = objects.prediction {
            $0.trip = lineTripTrunk2
            $0.departureTime = now.toKotlinInstant()
            $0.stopSequence = Int32(stopSequence)
        }
        let lineTripBranch = objects.trip(routePattern: linePatternBranch)
        let linePredictionBranch = objects.prediction {
            $0.trip = lineTripBranch
            $0.departureTime = now.toKotlinInstant()
            $0.stopSequence = Int32(stopSequence)
        }

        let basicPatternsByStop = PatternsByStop(route: route, stop: stop, patterns: [
            RealtimePatterns.ByHeadsign(
                route: route,
                headsign: "North",
                line: nil,
                patterns: [patternNorth],
                upcomingTrips: [objects.upcomingTrip(prediction: predictionNorth)],
                alertsHere: nil,
                hasSchedulesToday: true
            ),
            RealtimePatterns.ByHeadsign(
                route: route,
                headsign: "South",
                line: nil,
                patterns: [patternSouth],
                upcomingTrips: [objects.upcomingTrip(prediction: predictionSouth)],
                alertsHere: nil,
                hasSchedulesToday: true
            ),
        ])

        let linePatternsByStop = PatternsByStop(
            routes: [lineRoute1, lineRoute2, lineRoute3],
            line: line,
            stop: stop,
            patterns: [
                RealtimePatterns.ByDirection(
                    line: line,
                    routes: [lineRoute1, lineRoute2],
                    direction: Direction(name: "Outbound", destination: "Trunk Destination", id: 0),
                    patterns: [linePatternTrunk1, linePatternTrunk2],
                    upcomingTrips: [
                        objects.upcomingTrip(prediction: linePredictionTrunk1),
                        objects.upcomingTrip(prediction: linePredictionTrunk2),
                    ],
                    alertsHere: nil,
                    hasSchedulesToday: true
                ),
                RealtimePatterns.ByHeadsign(
                    route: lineRoute3,
                    headsign: "Branch",
                    line: line,
                    patterns: [linePatternBranch],
                    upcomingTrips: [objects.upcomingTrip(prediction: linePredictionBranch)],
                    alertsHere: nil,
                    hasSchedulesToday: true
                ),
            ],
            directions: [
                Direction(name: "Outbound", destination: nil, id: 0),
                Direction(name: "", destination: "", id: 1),
            ]
        )

        let departures = StopDetailsDepartures(routes: [basicPatternsByStop, linePatternsByStop])

        return .init(
            departures: departures,
            routeId: route.id,
            lineId: line.id,
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
            global: nil,
            now: data.now,
            filter: .init(routeId: data.routeId, directionId: 0),
            setFilter: { _ in },
            pushNavEntry: { _ in },
            pinned: false
        )

        XCTAssertNotNil(try sut.inspect().find(text: "North"))
        XCTAssertNil(try? sut.inspect().find(text: "South"))
    }

    func testDisplaysLine() throws {
        let data = testData()

        let sut = StopDetailsFilteredRouteView(
            departures: data.departures,
            global: nil,
            now: data.now,
            filter: .init(routeId: data.lineId, directionId: 0),
            setFilter: { _ in },
            pushNavEntry: { _ in },
            pinned: false
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Trunk 1"))
        XCTAssertNotNil(try sut.inspect().find(text: "Trunk 2"))
        XCTAssertNotNil(try sut.inspect().find(text: "Branch"))
    }

    func testLinks() throws {
        let data = testData()

        let pushExp = XCTestExpectation(description: "pushNavEntry called with expected trip details")

        let expected = SheetNavigationStackEntry.tripDetails(
            tripId: data.tripNorthId,
            vehicleId: data.vehicleNorthId,
            target: .init(stopId: data.stopId, stopSequence: data.stopSequence),
            routeId: data.routeId,
            directionId: 0
        )

        func pushNavEntry(entry: SheetNavigationStackEntry) {
            if entry == expected {
                pushExp.fulfill()
            }
        }

        let sut = StopDetailsFilteredRouteView(
            departures: data.departures,
            global: nil,
            now: data.now,
            filter: .init(routeId: data.routeId, directionId: 0),
            setFilter: { _ in },
            pushNavEntry: pushNavEntry,
            pinned: false
        )

        try sut.inspect().find(button: "North").tap()

        wait(for: [pushExp], timeout: 1)
    }
}

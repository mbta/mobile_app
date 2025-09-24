//
//  RouteCardDeparturesTests.swift
//  iosApp
//
//  Created by esimon on 4/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class RouteCardDeparturesTests: XCTestCase {
    func testShowsServiceEnded() throws {
        let objects = ObjectCollectionBuilder()

        let route = objects.route { route in
            route.longName = "1"
            route.type = .bus
        }
        let pattern = objects.routePattern(route: route) { _ in }
        let stop = objects.stop { stop in
            stop.name = "Stop Name"
        }

        let stopData = RouteCardData.RouteStopData(
            lineOrRoute: .route(route),
            stop: stop,
            directions: [.init(name: "Outbound", destination: "Harvard", id: 0)],
            data: [.init(
                lineOrRoute: .route(route), stop: stop,
                directionId: 0, routePatterns: [pattern], stopIds: [stop.id],
                upcomingTrips: [], alertsHere: [], allDataLoaded: true,
                hasSchedulesToday: true, alertsDownstream: [],
                context: .nearbyTransit
            )]
        )

        let sut = RouteCardDepartures(
            stopData: stopData,
            global: .init(objects: objects),
            now: EasternTimeInstant.now(),
            isFavorite: { _ in false },
            pushNavEntry: { _ in }
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Outbound to"))
        XCTAssertNotNil(try sut.inspect().find(text: "Harvard"))
        XCTAssertNotNil(try sut.inspect().find(text: "Service ended"))
    }

    func testShowsNoPredictions() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant.now()
        let route = objects.route { route in
            route.longName = "Orange Line"
            route.type = .heavyRail
            route.directionNames = ["South", "North"]
            route.directionDestinations = ["Forest Hills", "Oak Grove"]
        }
        let pattern = objects.routePattern(route: route) { _ in }
        let stop = objects.stop { stop in
            stop.name = "Stop Name"
        }
        let trip = objects.trip(routePattern: pattern)
        let schedule = objects.schedule { schedule in
            schedule.departureTime = now.plus(minutes: 10)
        }

        let stopData = RouteCardData.RouteStopData(
            lineOrRoute: .route(route),
            stop: stop,
            directions: [.init(name: "South", destination: "Forest Hills", id: 0)],
            data: [.init(
                lineOrRoute: .route(route), stop: stop,
                directionId: 0, routePatterns: [pattern], stopIds: [stop.id],
                upcomingTrips: [.init(trip: trip, schedule: schedule)], alertsHere: [], allDataLoaded: true,
                hasSchedulesToday: true, alertsDownstream: [],
                context: .nearbyTransit
            )]
        )

        let sut = RouteCardDepartures(
            stopData: stopData,
            global: .init(objects: objects),
            now: now,
            isFavorite: { _ in false },
            pushNavEntry: { _ in }
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Southbound to"))
        XCTAssertNotNil(try sut.inspect().find(text: "Forest Hills"))
        XCTAssertNotNil(try sut.inspect().find(text: "Predictions unavailable"))
    }

    func testShowsBranchingHeadsigns() throws {
        typealias Green = GreenLineTestHelper.Companion
        let now = EasternTimeInstant.now()
        let objects = Green.shared.objects

        let stop = objects.stop { _ in }
        let downstreamAlert = objects.alert { alert in
            alert.effect = .shuttle
        }

        let tripB0 = objects.trip(routePattern: Green.shared.rpB0)
        let schedB0 = objects.schedule { schedule in
            schedule.arrivalTime = now.plus(minutes: 1).plus(seconds: 1)
            schedule.departureTime = now.plus(minutes: 2)
            schedule.stopId = Green.shared.stopWestbound.id
            schedule.trip = tripB0
        }

        let tripB1 = objects.trip(routePattern: Green.shared.rpB1)
        let schedB1 = objects.schedule { schedule in
            schedule.arrivalTime = now.plus(minutes: 2).plus(seconds: 10)
            schedule.departureTime = now.plus(minutes: 3)
            schedule.stopId = Green.shared.stopEastbound.id
            schedule.trip = tripB1
        }

        let predB0 = objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: 1).plus(seconds: 1)
            prediction.departureTime = now.plus(minutes: 2)
            prediction.routeId = Green.shared.routeB.id
            prediction.stopId = Green.shared.stopWestbound.id
            prediction.tripId = tripB0.id
        }
        let predB1 = objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: 2).plus(seconds: 10)
            prediction.departureTime = now.plus(minutes: 3)
            prediction.routeId = Green.shared.routeB.id
            prediction.stopId = Green.shared.stopEastbound.id
            prediction.tripId = tripB1.id
        }

        let tripC01 = objects.trip(routePattern: Green.shared.rpC0)
        let predC01 = objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: 3)
            prediction.departureTime = now.plus(minutes: 4)
            prediction.routeId = Green.shared.routeC.id
            prediction.stopId = Green.shared.stopWestbound.id
            prediction.tripId = tripC01.id
        }
        let tripC02 = objects.trip(routePattern: Green.shared.rpC0)
        let predC02 = objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: 11)
            prediction.departureTime = now.plus(minutes: 15)
            prediction.status = "Overridden"
            prediction.routeId = Green.shared.routeC.id
            prediction.stopId = Green.shared.stopWestbound.id
            prediction.tripId = tripC02.id
        }

        let tripC11 = objects.trip(routePattern: Green.shared.rpC1)
        let predC11 = objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: 4)
            prediction.departureTime = now.plus(minutes: 5)
            prediction.routeId = Green.shared.routeC.id
            prediction.stopId = Green.shared.stopEastbound.id
            prediction.tripId = tripC11.id
        }

        let tripC12 = objects.trip(routePattern: Green.shared.rpC1)
        let predC12 = objects.prediction { prediction in
            prediction.departureTime = now.plus(minutes: 10)
            prediction.routeId = Green.shared.routeC.id
            prediction.stopId = Green.shared.stopEastbound.id
            prediction.tripId = tripC12.id
        }

        let tripE0 = objects.trip(routePattern: Green.shared.rpE0)
        let predE0 = objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: 5)
            prediction.departureTime = now.plus(minutes: 6)
            prediction.routeId = Green.shared.routeE.id
            prediction.stopId = Green.shared.stopWestbound.id
            prediction.tripId = tripE0.id
        }

        let tripE1 = objects.trip(routePattern: Green.shared.rpE1)
        let predE1 = objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: 6)
            prediction.departureTime = now.plus(minutes: 7)
            prediction.routeId = Green.shared.routeE.id
            prediction.stopId = Green.shared.stopEastbound.id
            prediction.tripId = tripE1.id
        }

        let lineOrRoute = LineOrRoute.line(
            Green.shared.line,
            [Green.shared.routeB, Green.shared.routeC, Green.shared.routeE]
        )

        let context = RouteCardData.Context.nearbyTransit
        let stopData = RouteCardData.RouteStopData(
            lineOrRoute: lineOrRoute,
            stop: stop,
            directions: [
                .init(name: "West", destination: "Copley & West", id: 0),
                .init(name: "East", destination: "Park St & North", id: 1),
            ],
            data: [.init(
                lineOrRoute: lineOrRoute,
                stop: stop,
                directionId: 0,
                routePatterns: [Green.shared.rpB0, Green.shared.rpC0, Green.shared.rpE0],
                stopIds: [stop.id],
                upcomingTrips: [
                    .init(trip: tripB0, schedule: schedB0, prediction: predB0),
                    .init(trip: tripC01, prediction: predC01),
                    .init(trip: tripE0, prediction: predE0),
                    .init(trip: tripC02, prediction: predC02),
                ], alertsHere: [], allDataLoaded: true,
                hasSchedulesToday: true, alertsDownstream: [downstreamAlert],
                context: context
            ),
            .init(
                lineOrRoute: lineOrRoute,
                stop: stop,
                directionId: 1,
                routePatterns: [Green.shared.rpB1, Green.shared.rpC1, Green.shared.rpE1],
                stopIds: [stop.id],
                upcomingTrips: [
                    .init(trip: tripB1, schedule: schedB1, prediction: predB1),
                    .init(trip: tripC11, prediction: predC11),
                    .init(trip: tripE1, prediction: predE1),
                    .init(trip: tripC12, prediction: predC12),
                ], alertsHere: [], allDataLoaded: true,
                hasSchedulesToday: true, alertsDownstream: [],
                context: context
            )]
        )

        let sut = RouteCardDepartures(
            stopData: stopData,
            global: .init(objects: objects),
            now: now,
            isFavorite: { _ in false },
            pushNavEntry: { _ in }
        )

        let westDirection = try sut.inspect().find(text: "Westbound to")
            .find(RouteCardDirection.self, relation: .parent)
        XCTAssertNotNil(westDirection)
        XCTAssertNotNil(try westDirection.find(viewWithAccessibilityLabel: "Alert"))
        XCTAssertNotNil(try westDirection.find(text: "1 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Boston College"))
        XCTAssertNotNil(try westDirection.find(text: "3 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Cleveland Circle"))
        XCTAssertNotNil(try westDirection.find(text: "5 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Heath Street"))
        XCTAssertThrowsError(try westDirection.find(text: "Overridden"))

        let eastDirection = try sut.inspect().find(text: "Eastbound to")
            .find(RouteCardDirection.self, relation: .parent)
        XCTAssertNotNil(eastDirection)
        XCTAssertNotNil(try eastDirection.find(text: "2 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Government Center"))
        XCTAssertNotNil(try eastDirection.find(text: "4 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Government Center"))
        XCTAssertNotNil(try eastDirection.find(text: "6 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Medford/Tufts"))
    }

    func testSinglePill() throws {
        let now = EasternTimeInstant.now()
        let objects = TestData.clone()

        let stop = objects.getStop(id: "place-rsmnl")
        let line = objects.getLine(id: "line-Green")
        let route = objects.getRoute(id: "Green-D")
        let routePattern = objects.getRoutePattern(id: "Green-D-855-0")

        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: routePattern)
            prediction.departureTime = now.plus(minutes: 5)
        })

        let lineOrRoute = LineOrRoute.line(line, [route])

        let context = RouteCardData.Context.nearbyTransit
        let stopData = RouteCardData.RouteStopData(
            lineOrRoute: lineOrRoute,
            stop: stop,
            directions: [
                .init(name: "West", destination: "Riverside", id: 0),
                .init(name: "East", destination: "Park St & North", id: 1),
            ],
            data: [.init(
                lineOrRoute: lineOrRoute,
                stop: stop,
                directionId: 0,
                routePatterns: [routePattern],
                stopIds: [stop.id],
                upcomingTrips: [trip],
                alertsHere: [], allDataLoaded: true,
                hasSchedulesToday: true, alertsDownstream: [],
                context: context
            )]
        )

        let sut = RouteCardDepartures(
            stopData: stopData,
            global: .init(objects: objects),
            now: now,
            isFavorite: { _ in false },
            pushNavEntry: { _ in }
        )

        XCTAssertNotNil(try sut.inspect().find(RoutePill.self))
    }

    func testAnalytics() throws {
        let now = EasternTimeInstant.now()
        let objects = TestData.clone()

        let stop = objects.getStop(id: "place-rsmnl")
        let line = objects.getLine(id: "line-Green")
        let route = objects.getRoute(id: "Green-D")
        let routePattern = objects.getRoutePattern(id: "Green-D-855-0")

        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: routePattern)
            prediction.departureTime = now.plus(minutes: 5)
        })

        let lineOrRoute = LineOrRoute.line(line, [route])

        let context = RouteCardData.Context.nearbyTransit
        let stopData = RouteCardData.RouteStopData(
            lineOrRoute: lineOrRoute,
            stop: stop,
            directions: [
                .init(name: "West", destination: "Riverside", id: 0),
                .init(name: "East", destination: "Park St & North", id: 1),
            ],
            data: [.init(
                lineOrRoute: lineOrRoute,
                stop: stop,
                directionId: 0,
                routePatterns: [routePattern],
                stopIds: [stop.id],
                upcomingTrips: [trip],
                alertsHere: [], allDataLoaded: true,
                hasSchedulesToday: true, alertsDownstream: [],
                context: context
            )]
        )

        var eventProps: [String: String] = [:]

        let sut = RouteCardDepartures(
            analytics: MockAnalytics(onLogEvent: { _, props in eventProps = props }),
            stopData: stopData,
            global: .init(objects: objects),
            now: now,
            isFavorite: { rsd in rsd == RouteStopDirection(route: lineOrRoute.id, stop: "place-rsmnl", direction: 0) },
            pushNavEntry: { _ in }
        )

        try sut.inspect().findAll(ViewType.Button.self)[0].tap()
        XCTAssertEqual(eventProps["pinned"], "true")
    }
}

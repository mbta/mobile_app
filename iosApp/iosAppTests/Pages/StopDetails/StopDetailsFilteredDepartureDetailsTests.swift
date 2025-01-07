//
//  StopDetailsFilteredDepartureDetailsTests.swift
//  iosAppTests
//
//  Created by esimon on 12/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsFilteredDepartureDetailsTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testDisplaysTrips() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()

        let tile1 = TileData(
            route: route,
            headsign: "A",
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "1", routeType: .heavyRail, format: .Arriving())],
                secondaryAlert: nil
            )
        )
        let tile2 = TileData(
            route: route,
            headsign: "B",
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "2", routeType: .heavyRail, format: .Minutes(minutes: 3))],
                secondaryAlert: nil
            )
        )
        let tile3 = TileData(
            route: route,
            headsign: "C",
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "3", routeType: .heavyRail, format: .Minutes(minutes: 7))],
                secondaryAlert: nil
            )
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile1, tile2, tile3],
            noPredictionsStatus: nil,
            alerts: [],
            patternsByStop: .init(route: route, stop: stop, patterns: []),
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())

        XCTAssertNotNil(try sut.inspect().find(text: "A"))
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(text: "B"))
        XCTAssertNotNil(try sut.inspect().find(text: "3 min"))
        XCTAssertNotNil(try sut.inspect().find(text: "C"))
        XCTAssertNotNil(try sut.inspect().find(text: "7 min"))
    }

    func testHeadsignsHiddenWhenMatching() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()

        let tile1 = TileData(
            route: route,
            headsign: "Matching",
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "1", routeType: .heavyRail, format: .Arriving())],
                secondaryAlert: nil
            )
        )
        let tile2 = TileData(
            route: route,
            headsign: tile1.headsign,
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "2", routeType: .heavyRail, format: .Minutes(minutes: 3))],
                secondaryAlert: nil
            )
        )
        let tile3 = TileData(
            route: route,
            headsign: tile1.headsign,
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "3", routeType: .heavyRail, format: .Minutes(minutes: 7))],
                secondaryAlert: nil
            )
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile1, tile2, tile3],
            noPredictionsStatus: nil,
            alerts: [],
            patternsByStop: .init(route: route, stop: stop, patterns: []),
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())

        XCTAssertThrowsError(try sut.inspect().find(DepartureTile.self).find(text: tile1.headsign))
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(text: "3 min"))
        XCTAssertNotNil(try sut.inspect().find(text: "7 min"))
    }

    func testAlwaysShowsHeadsignAndPillsWhenLine() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { route in route.id = "Green-B" }
        let line = objects.line { line in line.id = "Green" }

        let tile1 = TileData(
            route: route,
            headsign: "Matching",
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "1", routeType: .lightRail, format: .Arriving())],
                secondaryAlert: nil
            )
        )
        let tile2 = TileData(
            route: route,
            headsign: tile1.headsign,
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "2", routeType: .lightRail, format: .Minutes(minutes: 3))],
                secondaryAlert: nil
            )
        )
        let tile3 = TileData(
            route: route,
            headsign: tile1.headsign,
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "3", routeType: .lightRail, format: .Minutes(minutes: 7))],
                secondaryAlert: nil
            )
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile1, tile2, tile3],
            noPredictionsStatus: nil,
            alerts: [],
            patternsByStop: .init(routes: [route], line: line, stop: stop, patterns: [], directions: []),
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())

        XCTAssertNotNil(try sut.inspect().find(DepartureTile.self).find(text: tile1.headsign))
        XCTAssertNotNil(try sut.inspect().find(DepartureTile.self).find(RoutePill.self))
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(text: "3 min"))
        XCTAssertNotNil(try sut.inspect().find(text: "7 min"))
    }

    func testShowsTripDetails() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { route in route.id = "Green-B" }
        let pattern = objects.routePattern(route: route) { _ in }
        let line = objects.line { line in line.id = "Green" }
        let trip = objects.trip { trip in
            trip.routeId = route.id
            trip.routePatternId = pattern.id
        }

        let tile1 = TileData(
            route: route,
            headsign: "Matching",
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: trip.id, routeType: .lightRail, format: .Arriving())],
                secondaryAlert: nil
            )
        )
        let tile2 = TileData(
            route: route,
            headsign: tile1.headsign,
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "2", routeType: .lightRail, format: .Minutes(minutes: 3))],
                secondaryAlert: nil
            )
        )
        let tile3 = TileData(
            route: route,
            headsign: tile1.headsign,
            formatted: RealtimePatterns.FormatSome(
                trips: [.init(id: "3", routeType: .lightRail, format: .Minutes(minutes: 7))],
                secondaryAlert: nil
            )
        )
        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: .init(tripId: trip.id, vehicleId: nil, stopSequence: nil, selectionLock: false),
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [tile1, tile2, tile3],
            noPredictionsStatus: nil,
            alerts: [],
            patternsByStop: .init(routes: [route], line: line, stop: stop, patterns: [], directions: []),
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())

        XCTAssertNotNil(try sut.inspect().find(TripDetailsView.self))
    }

    func testShowsNoTripCard() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { route in route.id = "Green-B" }
        let line = objects.line { line in line.id = "Green" }

        let sut = StopDetailsFilteredDepartureDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil,
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            tiles: [],
            noPredictionsStatus: RealtimePatterns.NoTripsFormatServiceEndedToday(),
            alerts: [],
            patternsByStop: .init(routes: [route], line: line, stop: stop, patterns: [], directions: []),
            pinned: false,
            now: Date.now,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: .init(),
            viewportProvider: .init()
        ).environmentObject(ViewportProvider())

        XCTAssertThrowsError(try sut.inspect().find(TripDetailsView.self))
        XCTAssertThrowsError(try sut.inspect().find(DepartureTile.self))
        XCTAssertNotNil(try sut.inspect().find(StopDetailsNoTripCard.self))
    }
}

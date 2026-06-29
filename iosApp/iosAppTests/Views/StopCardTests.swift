//
//  StopCardTests.swift
//  iosApp
//
//  Created by Melody Horn on 6/25/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class StopCardTests: XCTestCase {
    func testRoutePill() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route { route in
            route.shortName = "66"
            route.type = .bus
        }

        let stopCardData = StopCardData(
            stop: stop,
            data: [.init(
                lineOrRoute: .route(route),
                stop: stop,
                direction: .init(directionId: 0, route: route),
                routePatterns: [],
                stopIds: [],
                upcomingTrips: [],
                alertsHere: [],
                allDataLoaded: true,
                hasSchedulesToday: true,
                subwayServiceStartTime: nil,
                alertsDownstream: [],
                context: .favorites
            )]
        )

        let sut = StopCard(
            cardData: stopCardData,
            global: .init(objects: objects),
            now: EasternTimeInstant.now(),
            isFavorite: { _ in false },
            pushNavEntry: { _ in }
        ).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: "66"))
    }

    func testStopHeader() throws {
        let objects = ObjectCollectionBuilder()

        let route = objects.route { route in
            route.longName = "1"
            route.type = .bus
        }
        let stop = objects.stop { stop in
            stop.name = "Stop Name"
        }

        let sut = StopCard(
            cardData: .init(
                stop: stop,
                data: [.init(
                    lineOrRoute: .route(route),
                    stop: stop,
                    direction: .init(directionId: 0, route: route),
                    routePatterns: [],
                    stopIds: [],
                    upcomingTrips: [],
                    alertsHere: [],
                    allDataLoaded: true,
                    hasSchedulesToday: true,
                    subwayServiceStartTime: nil,
                    alertsDownstream: [],
                    context: .favorites
                )]
            ),
            global: .init(objects: objects),
            now: EasternTimeInstant.now(),
            isFavorite: { _ in false },
            pushNavEntry: { _ in }
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(text: stop.name))
    }

    func testShowsDepartures() throws {
        let objects = ObjectCollectionBuilder()

        let route = objects.route { route in
            route.longName = "1"
            route.type = .bus
        }
        let pattern = objects.routePattern(route: route) { _ in }
        let stop = objects.stop { stop in
            stop.name = "Stop Name"
        }

        let sut = StopCard(
            cardData: .init(
                stop: stop,
                data: [.init(
                    lineOrRoute: .route(route), stop: stop,
                    direction: .init(name: "Inbound", destination: "", id: 0), routePatterns: [pattern],
                    stopIds: [stop.id],
                    upcomingTrips: [], alertsHere: [], allDataLoaded: true,
                    hasSchedulesToday: true, subwayServiceStartTime: nil, alertsDownstream: [],
                    context: .favorites
                )]
            ),
            global: .init(objects: objects),
            now: EasternTimeInstant.now(),
            isFavorite: { _ in false },
            pushNavEntry: { _ in }
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(StopCardDepartures.self))
        XCTAssertNotNil(try sut.inspect().find(text: "Inbound to"))
    }

    func testBranchDisrupted() throws {
        let data = RouteCardPreviewData()
        let cardData = try XCTUnwrap(StopCardData.companion.fromRouteCardData(
            routeCardData: [data.GL5()],
            sortByDistanceFrom: nil
        ).first)
        let sut = StopCard(
            cardData: cardData,
            global: data.global,
            now: data.now,
            isFavorite: { _ in false },
            pushNavEntry: { _ in }
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(StopCardDepartures.self))
        let westDirection = try sut.inspect().find(text: "Green Line Westbound to")
            .find(StopCardDirection.self, relation: .parent)
        XCTAssertNotNil(westDirection)
        XCTAssertNotNil(try westDirection.find(text: "3 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Cleveland Circle"))
        XCTAssertNotNil(try westDirection.find(text: "5 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Boston College"))
        XCTAssertNotNil(try westDirection.find(text: "Shuttle Bus")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Riverside"))

        let eastDirection = try sut.inspect().find(text: "Green Line Eastbound to")
            .find(StopCardDirection.self, relation: .parent)
        XCTAssertNotNil(eastDirection)
        XCTAssertNotNil(try eastDirection.find(text: "6 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Government Center"))
        XCTAssertNotNil(try eastDirection.find(text: "12 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Government Center"))
    }
}

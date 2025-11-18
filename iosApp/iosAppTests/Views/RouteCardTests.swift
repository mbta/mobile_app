//
//  RouteCardTests.swift
//  iosAppTests
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

final class RouteCardTests: XCTestCase {
    func testRouteHeader() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.shortName = "66"
            route.type = .bus
        }

        let routeCardData = RouteCardData(
            lineOrRoute: .route(route),
            stopData: [],
            at: EasternTimeInstant.now()
        )

        let sut = RouteCard(
            cardData: routeCardData,
            global: .init(objects: objects),
            now: EasternTimeInstant.now(),
            isFavorite: { _ in false },
            pushNavEntry: { _ in },
            showStopHeader: true
        ).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: "66"))
        XCTAssertNotNil(try? sut.inspect().find(imageName: "mode-bus"))
    }

    func testLineHeader() throws {
        let objects = ObjectCollectionBuilder()
        let line = objects.line { line in
            line.longName = "Green Line"
        }
        let route = objects.route { route in
            route.longName = "Green Line - C"
            route.type = .lightRail
        }

        let routeCardData = RouteCardData(
            lineOrRoute: .line(line, [route]),
            stopData: [],
            at: EasternTimeInstant.now()
        )

        let sut = RouteCard(
            cardData: routeCardData,
            global: .init(objects: objects),
            now: EasternTimeInstant.now(),
            isFavorite: { _ in false },
            pushNavEntry: { _ in },
            showStopHeader: true
        ).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: "Green Line"))
        XCTAssertNotNil(try? sut.inspect().find(imageName: "mode-subway"))
    }

    func testPinHiddenWhenEnhanced() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.longName = "Red"
        }

        let routeCardData = RouteCardData(
            lineOrRoute: .route(route),
            stopData: [],
            at: EasternTimeInstant.now()
        )

        let sut = RouteCard(
            cardData: routeCardData,
            global: .init(objects: objects),
            now: EasternTimeInstant.now(),
            isFavorite: { _ in false },
            pushNavEntry: { _ in },
            showStopHeader: true
        ).withFixedSettings([:])

        XCTAssertThrowsError(
            try sut.inspect().find(viewWithAccessibilityIdentifier: "starButton")
        )
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

        let nearbySut = RouteCard(
            cardData: RouteCardData(
                lineOrRoute: .route(route),
                stopData: [.init(
                    lineOrRoute: .route(route),
                    stop: stop,
                    directions: [],
                    data: []
                )],
                at: EasternTimeInstant.now()
            ),
            global: .init(objects: objects),
            now: EasternTimeInstant.now(),
            isFavorite: { _ in false },
            pushNavEntry: { _ in },
            showStopHeader: true
        ).withFixedSettings([:])
        XCTAssertNotNil(try nearbySut.inspect().find(text: stop.name))

        let stopDetailsSut = RouteCard(
            cardData: RouteCardData(
                lineOrRoute: .route(route),
                stopData: [.init(
                    lineOrRoute: .route(route),
                    stop: stop,
                    directions: [],
                    data: []
                )],
                at: EasternTimeInstant.now()
            ),
            global: .init(objects: objects),
            now: EasternTimeInstant.now(),
            isFavorite: { _ in false },
            pushNavEntry: { _ in },
            showStopHeader: false
        ).withFixedSettings([:])
        XCTAssertThrowsError(try stopDetailsSut.inspect().find(text: stop.name))
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

        let sut = RouteCard(
            cardData: RouteCardData(
                lineOrRoute: .route(route),
                stopData: [.init(
                    lineOrRoute: .route(route),
                    stop: stop,
                    directions: [.init(name: "Inbound", destination: "", id: 0)],
                    data: [.init(
                        lineOrRoute: .route(route), stop: stop,
                        directionId: 0, routePatterns: [pattern], stopIds: [stop.id],
                        upcomingTrips: [], alertsHere: [], allDataLoaded: true,
                        hasSchedulesToday: true, subwayServiceStartTime: nil, alertsDownstream: [],
                        context: .nearbyTransit
                    )]
                )],
                at: EasternTimeInstant.now()
            ),
            global: .init(objects: objects),
            now: EasternTimeInstant.now(),
            isFavorite: { _ in false },
            pushNavEntry: { _ in },
            showStopHeader: true
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(RouteCardDepartures.self))
        XCTAssertNotNil(try sut.inspect().find(text: "Inbound to"))
    }

    func testBranchDisrupted() throws {
        let data = RouteCardPreviewData()
        let sut = RouteCard(
            cardData: data.GL5(),
            global: data.global,
            now: data.now,
            isFavorite: { _ in false },
            pushNavEntry: { _ in },
            showStopHeader: true
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(RouteCardDepartures.self))
        let westDirection = try sut.inspect().find(text: "Westbound to")
            .find(RouteCardDirection.self, relation: .parent)
        XCTAssertNotNil(westDirection)
        XCTAssertNotNil(try westDirection.find(text: "3 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Cleveland Circle"))
        XCTAssertNotNil(try westDirection.find(text: "5 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Boston College"))
        XCTAssertNotNil(try westDirection.find(text: "Shuttle Bus")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Riverside"))

        let eastDirection = try sut.inspect().find(text: "Eastbound to")
            .find(RouteCardDirection.self, relation: .parent)
        XCTAssertNotNil(eastDirection)
        XCTAssertNotNil(try eastDirection.find(text: "6 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Government Center"))
        XCTAssertNotNil(try eastDirection.find(text: "12 min")
            .find(HeadsignRowView.self, relation: .parent).find(text: "Government Center"))
    }
}

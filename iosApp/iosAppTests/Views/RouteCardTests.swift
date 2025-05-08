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
            context: .nearbyTransit,
            at: Date.now.toKotlinInstant()
        )

        let sut = RouteCard(
            cardData: routeCardData,
            global: .init(objects: objects),
            now: Date.now,
            onPin: { _ in },
            pinned: false,
            pushNavEntry: { _ in },
            showStationAccessibility: false
        )

        XCTAssertNotNil(try sut.inspect().find(text: "66"))
        XCTAssertNotNil(try? sut.inspect().find(ViewType.Image.self) { image in
            try image.actualImage().name() == "mode-bus"
        })
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
            context: .nearbyTransit,
            at: Date.now.toKotlinInstant()
        )

        let sut = RouteCard(
            cardData: routeCardData,
            global: .init(objects: objects),
            now: Date.now,
            onPin: { _ in },
            pinned: false,
            pushNavEntry: { _ in },
            showStationAccessibility: false
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Green Line"))
        XCTAssertNotNil(try? sut.inspect().find(ViewType.Image.self) { image in
            try image.actualImage().name() == "mode-subway"
        })
    }

    func testPinRoute() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.longName = "Red"
        }

        let routeCardData = RouteCardData(
            lineOrRoute: .route(route),
            stopData: [],
            context: .nearbyTransit,
            at: Date.now.toKotlinInstant()
        )

        let pinRouteExp = XCTestExpectation(description: "pinRoute called")

        func onPin(_: String) {
            pinRouteExp.fulfill()
        }

        let sut = RouteCard(
            cardData: routeCardData,
            global: .init(objects: objects),
            now: Date.now,
            onPin: onPin,
            pinned: false,
            pushNavEntry: { _ in },
            showStationAccessibility: false
        )

        let button =
            try sut.inspect().find(viewWithAccessibilityIdentifier: "pinButton").button()

        try button.tap()
        wait(for: [pinRouteExp], timeout: 1)
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
                    data: [],
                    context: .nearbyTransit
                )],
                context: .nearbyTransit,
                at: Date.now.toKotlinInstant()
            ),
            global: .init(objects: objects),
            now: Date.now,
            onPin: { _ in },
            pinned: false,
            pushNavEntry: { _ in },
            showStationAccessibility: false
        )
        XCTAssertNotNil(try nearbySut.inspect().find(text: stop.name))

        let stopDetailsSut = RouteCard(
            cardData: RouteCardData(
                lineOrRoute: .route(route),
                stopData: [.init(
                    lineOrRoute: .route(route),
                    stop: stop,
                    directions: [],
                    data: [],
                    context: .stopDetailsUnfiltered
                )],
                context: .stopDetailsUnfiltered,
                at: Date.now.toKotlinInstant()
            ),
            global: .init(objects: objects),
            now: Date.now,
            onPin: { _ in },
            pinned: false,
            pushNavEntry: { _ in },
            showStationAccessibility: false
        )
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
                        directionId: 0, routePatterns: [pattern], stopIds: [stop.id],
                        upcomingTrips: [], alertsHere: [], allDataLoaded: true,
                        hasSchedulesToday: true, alertsDownstream: []
                    )],
                    context: .nearbyTransit
                )],
                context: .nearbyTransit,
                at: Date.now.toKotlinInstant()
            ),
            global: .init(objects: objects),
            now: Date.now,
            onPin: { _ in },
            pinned: false,
            pushNavEntry: { _ in },
            showStationAccessibility: false
        )
        XCTAssertNotNil(try sut.inspect().find(RouteCardDepartures.self))
        XCTAssertNotNil(try sut.inspect().find(text: "Inbound to"))
    }

    func testBranchDisrupted() throws {
        let data = RouteCardPreviewData()
        let sut = RouteCard(
            cardData: data.GL5(),
            global: data.global,
            now: data.now.toNSDate(),
            onPin: { _ in },
            pinned: false,
            pushNavEntry: { _ in },
            showStationAccessibility: false
        )
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

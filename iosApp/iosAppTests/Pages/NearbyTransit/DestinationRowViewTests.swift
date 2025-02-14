//
//  DestinationRowViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2025-02-14.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import ViewInspector
import XCTest

final class DestinationRowViewTests: XCTestCase {
    func testSkipsChevronOnDisruption() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let line = objects.line()
        let route = objects.route()
        let pattern = objects.routePattern(route: route) { _ in }
        let alert = objects.alert { $0.effect = .shuttle }
        let now = Date.now.toKotlinInstant()
        let dataByHeadsign = RealtimePatterns.ByHeadsign(
            route: route,
            headsign: "A",
            line: nil,
            patterns: [pattern],
            upcomingTrips: [],
            alertsHere: [alert]
        )
        let sutByHeadsign = DestinationRowView(
            patterns: dataByHeadsign,
            stop: stop,
            routeId: route.id,
            now: now,
            context: .nearbyTransit,
            pushNavEntry: { _ in },
            analytics: MockAnalytics(),
            pinned: false,
            routeType: route.type
        )
        XCTAssertThrowsError(try sutByHeadsign.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-chevron-right"
        }))
        let dataByDirection = RealtimePatterns.ByDirection(
            line: line,
            routes: [route],
            direction: .init(name: "", destination: "", id: 0),
            patterns: [pattern],
            upcomingTrips: [],
            alertsHere: [alert]
        )
        let sutByDirection = DestinationRowView(
            patterns: dataByDirection,
            stop: stop,
            routeId: route.id,
            now: now,
            context: .nearbyTransit,
            pushNavEntry: { _ in },
            analytics: MockAnalytics(),
            pinned: false,
            routeType: route.type
        )
        XCTAssertThrowsError(try sutByDirection.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "fa-chevron-right"
        }))
    }
}

//
//  WorldCupBlurbTests.swift
//  iosApp
//
//  Created by Melody Horn on 4/6/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Foundation

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class WorldCupBlurbTests: XCTestCase {
    func testShowsBlurbOutbound() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let sut = WorldCupBlurb(
            leaf: .init(lineOrRoute: .Route(route: WorldCupService.shared.route), stop: stop, directionId: 0,
                        routePatterns: [WorldCupService.shared.routePatternOutbound], stopIds: [], upcomingTrips: [],
                        alertsHere: [], allDataLoaded: true, hasSchedulesToday: false, subwayServiceStartTime: nil,
                        alertsDownstream: [], context: .nearbyTransit),
            routeAccents: .init(route: WorldCupService.shared.route),
            offerDetails: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Service from South Station to today’s World Cup match"))
        XCTAssertNotNil(try sut.inspect().find(text: "Boston Stadium Train ticket required"))
    }

    func testShowsBlurbInbound() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let sut = WorldCupBlurb(
            leaf: .init(lineOrRoute: .Route(route: WorldCupService.shared.route), stop: stop, directionId: 1,
                        routePatterns: [WorldCupService.shared.routePatternInbound], stopIds: [], upcomingTrips: [],
                        alertsHere: [], allDataLoaded: true, hasSchedulesToday: false, subwayServiceStartTime: nil,
                        alertsDownstream: [], context: .nearbyTransit),
            routeAccents: .init(route: WorldCupService.shared.route),
            offerDetails: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Service from today’s World Cup match to South Station"))
        XCTAssertNotNil(try sut.inspect().find(text: "Boston Stadium Train ticket required"))
    }

    func testHidesViewDetails() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let sut = WorldCupBlurb(
            leaf: .init(lineOrRoute: .Route(route: WorldCupService.shared.route), stop: stop, directionId: 0,
                        routePatterns: [WorldCupService.shared.routePatternOutbound], stopIds: [], upcomingTrips: [],
                        alertsHere: [], allDataLoaded: true, hasSchedulesToday: false, subwayServiceStartTime: nil,
                        alertsDownstream: [], context: .nearbyTransit),
            routeAccents: .init(route: WorldCupService.shared.route),
            offerDetails: false
        )
        XCTAssertThrowsError(try sut.inspect().find(text: "View details"))
    }

    func testShowsViewDetails() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let sut = WorldCupBlurb(
            leaf: .init(lineOrRoute: .Route(route: WorldCupService.shared.route), stop: stop, directionId: 0,
                        routePatterns: [WorldCupService.shared.routePatternOutbound], stopIds: [], upcomingTrips: [],
                        alertsHere: [], allDataLoaded: true, hasSchedulesToday: false, subwayServiceStartTime: nil,
                        alertsDownstream: [], context: .nearbyTransit),
            routeAccents: .init(route: WorldCupService.shared.route),
            offerDetails: true
        )
        XCTAssertNotNil(try sut.inspect().find(text: "View details"))
    }
}

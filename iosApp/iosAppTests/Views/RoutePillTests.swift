//
//  RoutePillTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 2/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class RoutePillTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testBus() throws {
        let bus = RoutePill(route: Route(
            id: "627",
            type: .bus,
            color: "FFC72C",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Bedford VA Hospital", "Alewife Station"],
            longName: "Bedford VA Hospital - Alewife Station via Hanscom Airport",
            shortName: "62/76",
            sortOrder: 50621,
            textColor: "000000",
            routePatternIds: nil
        ), type: .flex)
        XCTAssertEqual(try bus.inspect().view(RoutePill.self).text().string(), "62/76")
    }

    @MainActor func testHeavyRail() throws {
        let red = RoutePill(route: Route(
            id: "Red",
            type: .heavyRail,
            color: "DA291C",
            directionNames: ["South", "North"],
            directionDestinations: ["Ashmont/Braintree", "Alewife"],
            longName: "Red Line",
            shortName: "",
            sortOrder: 10010,
            textColor: "FFFFFF",
            routePatternIds: nil
        ), type: .flex)
        let blue = RoutePill(route: Route(
            id: "Blue",
            type: .heavyRail,
            color: "003DA5",
            directionNames: ["West", "East"],
            directionDestinations: ["Bowdoin", "Wonderland"],
            longName: "Blue Line",
            shortName: "",
            sortOrder: 10040,
            textColor: "FFFFFF",
            routePatternIds: nil
        ), type: .flex)

        XCTAssertEqual(try red.inspect().view(RoutePill.self).text().string(), "Red Line")
        XCTAssertEqual(try blue.inspect().view(RoutePill.self).text().string(), "Blue Line")
    }

    @MainActor func testLightRail() throws {
        let green = RoutePill(route: Route(
            id: "Green-C",
            type: .lightRail,
            color: "00843D",
            directionNames: ["West", "East"],
            directionDestinations: ["Cleveland Circle", "Government Center"],
            longName: "Green Line C",
            shortName: "C",
            sortOrder: 10033,
            textColor: "FFFFFF",
            routePatternIds: nil
        ), type: .flex)

        let mattapan = RoutePill(route: Route(
            id: "Mattapan",
            type: .lightRail,
            color: "DA291C",

            directionNames: [
                "Outbound",
                "Inbound",
            ],
            directionDestinations: [
                "Mattapan",
                "Ashmont",
            ],
            longName: "Mattapan Trolley",
            shortName: "",
            sortOrder: 10011,
            textColor: "FFFFFF",
            routePatternIds: nil
        ), type: .flex)

        XCTAssertEqual(try green.inspect().view(RoutePill.self).text().string(), "C")
        XCTAssertEqual(try mattapan.inspect().view(RoutePill.self).text().string(), "Mattapan Trolley")
    }

    @MainActor func testCommuterRail() throws {
        let middleborough = RoutePill(route: Route(
            id: "CR-Middleborough",
            type: .commuterRail,
            color: "80276C",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Middleborough/Lakeville", "South Station"],
            longName: "Middleborough/Lakeville Line",
            shortName: "",
            sortOrder: 20009,
            textColor: "FFFFFF",
            routePatternIds: nil
        ), type: .flex)
        let providence = RoutePill(route: Route(
            id: "CR-Providence",
            type: .commuterRail,
            color: "80276C",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Stoughton or Wickford Junction", "South Station"],
            longName: "Providence/Stoughton Line",
            shortName: "",
            sortOrder: 20012,
            textColor: "FFFFFF",
            routePatternIds: nil
        ), type: .flex)

        XCTAssertEqual(try middleborough.inspect().view(RoutePill.self).text().string(), "Middleborough/Lakeville Line")
        XCTAssertEqual(try providence.inspect().view(RoutePill.self).text().string(), "Providence/Stoughton Line")
    }

    @MainActor func testFerry() throws {
        let ferry = RoutePill(route: Route(
            id: "Boat-F1",
            type: .ferry,
            color: "008EAA",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Hingham or Hull", "Long Wharf or Rowes Wharf"],
            longName: "Hingham/Hull Ferry",
            shortName: "",
            sortOrder: 30002,
            textColor: "FFFFFF",
            routePatternIds: nil
        ), type: .flex)

        XCTAssertEqual(try ferry.inspect().view(RoutePill.self).text().string(), "Hingham/Hull Ferry")
    }

    @MainActor func testShuttle() throws {
        let rlShuttle = RoutePill(route: Route(
            id: "Shuttle-BroadwayKendall",
            type: .bus,
            color: "FFC72C",
            directionNames: ["South", "North"],
            directionDestinations: ["Ashmont/Braintree", "Alewife"],
            longName: "Kendall/MIT - Broadway via Downtown Crossing",
            shortName: "Red Line Shuttle",
            sortOrder: 61050,
            textColor: "000000",
            routePatternIds: nil
        ), type: .flex)
        let glShuttle = RoutePill(route: Route(
            id: "Shuttle-BrooklineHillsKenmore",
            type: .bus,
            color: "FFC72C",
            directionNames: ["West", "East"],
            directionDestinations: ["Riverside", "Union Square"],
            longName: "Brookline Hills - Kenmore",
            shortName: "Green Line D Shuttle",
            sortOrder: 61100,
            textColor: "000000",
            routePatternIds: nil
        ), type: .flex)
        XCTAssertEqual(try rlShuttle.inspect().view(RoutePill.self).text().string(), "Red Line Shuttle")
        XCTAssertEqual(try glShuttle.inspect().view(RoutePill.self).text().string(), "Green Line D Shuttle")
    }
}

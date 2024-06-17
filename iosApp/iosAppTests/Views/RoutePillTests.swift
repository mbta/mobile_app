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
        let busRoute = Route(
            id: "627",
            type: .bus,
            color: "FFC72C",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Bedford VA Hospital", "Alewife Station"],
            longName: "Bedford VA Hospital - Alewife Station via Hanscom Airport",
            shortName: "62/76",
            sortOrder: 50621,
            textColor: "000000",
            lineId: "line-6276",
            routePatternIds: nil
        )
        let fixedPill = RoutePill(route: busRoute, type: .fixed)
        XCTAssertEqual(try fixedPill.inspect().view(RoutePill.self).text().string(), "62/76")
        let flexPill = RoutePill(route: busRoute, type: .flex)
        XCTAssertEqual(try flexPill.inspect().view(RoutePill.self).text().string(), "62/76")
    }

    @MainActor func testHeavyRail() throws {
        let redLine = Route(
            id: "Red",
            type: .heavyRail,
            color: "DA291C",
            directionNames: ["South", "North"],
            directionDestinations: ["Ashmont/Braintree", "Alewife"],
            longName: "Red Line",
            shortName: "",
            sortOrder: 10010,
            textColor: "FFFFFF",
            lineId: "line-Red",
            routePatternIds: nil
        )
        let blueLine = Route(
            id: "Blue",
            type: .heavyRail,
            color: "003DA5",
            directionNames: ["West", "East"],
            directionDestinations: ["Bowdoin", "Wonderland"],
            longName: "Blue Line",
            shortName: "",
            sortOrder: 10040,
            textColor: "FFFFFF",
            lineId: "line-Blue",
            routePatternIds: nil
        )

        let redLineFixed = RoutePill(route: redLine, type: .fixed)
        let redLineFlex = RoutePill(route: redLine, type: .flex)
        let blueLineFixed = RoutePill(route: blueLine, type: .fixed)
        let blueLineFlex = RoutePill(route: blueLine, type: .flex)

        XCTAssertEqual(try redLineFixed.inspect().view(RoutePill.self).text().string(), "RL")
        XCTAssertEqual(try redLineFlex.inspect().view(RoutePill.self).text().string(), "Red Line")
        XCTAssertEqual(try blueLineFixed.inspect().view(RoutePill.self).text().string(), "BL")
        XCTAssertEqual(try blueLineFlex.inspect().view(RoutePill.self).text().string(), "Blue Line")
    }

    @MainActor func testLightRail() throws {
        let greenLineC = Route(
            id: "Green-C",
            type: .lightRail,
            color: "00843D",
            directionNames: ["West", "East"],
            directionDestinations: ["Cleveland Circle", "Government Center"],
            longName: "Green Line C",
            shortName: "C",
            sortOrder: 10033,
            textColor: "FFFFFF",
            lineId: "line-Green",
            routePatternIds: nil
        )

        let mattapan = Route(
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
            lineId: "line-Mattapan",
            routePatternIds: nil
        )

        let greenLineCFixed = RoutePill(route: greenLineC, type: .fixed)
        let greenLineCFlex = RoutePill(route: greenLineC, type: .flex)
        let mattapanFixed = RoutePill(route: mattapan, type: .fixed)
        let mattapanFlex = RoutePill(route: mattapan, type: .flex)

        XCTAssertEqual(try greenLineCFixed.inspect().view(RoutePill.self).text().string(), "GL C")
        XCTAssertEqual(try greenLineCFlex.inspect().view(RoutePill.self).text().string(), "C")
        XCTAssertEqual(try mattapanFixed.inspect().view(RoutePill.self).text().string(), "M")
        XCTAssertEqual(try mattapanFlex.inspect().view(RoutePill.self).text().string(), "Mattapan Trolley")
    }

    @MainActor func testCommuterRail() throws {
        let middleborough = Route(
            id: "CR-Middleborough",
            type: .commuterRail,
            color: "80276C",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Middleborough/Lakeville", "South Station"],
            longName: "Middleborough/Lakeville Line",
            shortName: "",
            sortOrder: 20009,
            textColor: "FFFFFF",
            lineId: "line-Middleborough",
            routePatternIds: nil
        )
        let providence = Route(
            id: "CR-Providence",
            type: .commuterRail,
            color: "80276C",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Stoughton or Wickford Junction", "South Station"],
            longName: "Providence/Stoughton Line",
            shortName: "",
            sortOrder: 20012,
            textColor: "FFFFFF",
            lineId: "line-Providence",
            routePatternIds: nil
        )

        let middleboroughFixed = RoutePill(route: middleborough, type: .fixed)
        let middleboroughFlex = RoutePill(route: middleborough, type: .flex)
        let providenceFixed = RoutePill(route: providence, type: .fixed)
        let providenceFlex = RoutePill(route: providence, type: .flex)

        XCTAssertEqual(try middleboroughFixed.inspect().view(RoutePill.self).text().string(), "CR")
        XCTAssertEqual(
            try middleboroughFlex.inspect().view(RoutePill.self).text().string(),
            "Middleborough/Lakeville Line"
        )
        XCTAssertEqual(try providenceFixed.inspect().view(RoutePill.self).text().string(), "CR")
        XCTAssertEqual(try providenceFlex.inspect().view(RoutePill.self).text().string(), "Providence/Stoughton Line")
    }

    @MainActor func testFerry() throws {
        let ferry = Route(
            id: "Boat-F1",
            type: .ferry,
            color: "008EAA",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Hingham or Hull", "Long Wharf or Rowes Wharf"],
            longName: "Hingham/Hull Ferry",
            shortName: "",
            sortOrder: 30002,
            textColor: "FFFFFF",
            lineId: "line-Boat-F1",
            routePatternIds: nil
        )

        let ferryFixed = RoutePill(route: ferry, type: .fixed)
        let ferryFlex = RoutePill(route: ferry, type: .flex)

        XCTAssertEqual(try ferryFixed.inspect().view(RoutePill.self).image().actualImage().name(), "mode-ferry")
        XCTAssertEqual(try ferryFlex.inspect().view(RoutePill.self).text().string(), "Hingham/Hull Ferry")
    }

    @MainActor func testShuttle() throws {
        let rlShuttle = Route(
            id: "Shuttle-BroadwayKendall",
            type: .bus,
            color: "FFC72C",
            directionNames: ["South", "North"],
            directionDestinations: ["Ashmont/Braintree", "Alewife"],
            longName: "Kendall/MIT - Broadway via Downtown Crossing",
            shortName: "Red Line Shuttle",
            sortOrder: 61050,
            textColor: "000000",
            lineId: "line-Red",
            routePatternIds: nil
        )
        let glShuttle = Route(
            id: "Shuttle-BrooklineHillsKenmore",
            type: .bus,
            color: "FFC72C",
            directionNames: ["West", "East"],
            directionDestinations: ["Riverside", "Union Square"],
            longName: "Brookline Hills - Kenmore",
            shortName: "Green Line D Shuttle",
            sortOrder: 61100,
            textColor: "000000",
            lineId: "line-Green",
            routePatternIds: nil
        )

        let rlShuttleFixed = RoutePill(route: rlShuttle, type: .fixed)
        let rlShuttleFlex = RoutePill(route: rlShuttle, type: .flex)
        let glShuttleFixed = RoutePill(route: glShuttle, type: .fixed)
        let glShuttleFlex = RoutePill(route: glShuttle, type: .flex)

        XCTAssertEqual(try rlShuttleFixed.inspect().view(RoutePill.self).text().string(), "Red Line Shuttle")
        XCTAssertEqual(try rlShuttleFlex.inspect().view(RoutePill.self).text().string(), "Red Line Shuttle")
        XCTAssertEqual(try glShuttleFixed.inspect().view(RoutePill.self).text().string(), "Green Line D Shuttle")
        XCTAssertEqual(try glShuttleFlex.inspect().view(RoutePill.self).text().string(), "Green Line D Shuttle")
    }
}

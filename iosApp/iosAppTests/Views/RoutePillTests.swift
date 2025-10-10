//
//  RoutePillTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 2/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class RoutePillTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testBus() throws {
        let busRoute = Route(
            id: .init("627"),
            type: .bus,
            color: "FFC72C",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Bedford VA Hospital", "Alewife Station"],
            isListedRoute: true,
            longName: "Bedford VA Hospital - Alewife Station via Hanscom Airport",
            shortName: "62/76",
            sortOrder: 50621,
            textColor: "000000",
            lineId: .init("line-6276"),
            routePatternIds: nil
        )
        let fixedPill = RoutePill(route: busRoute, type: .fixed)
        XCTAssertEqual(
            try fixedPill.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "62/76"
        )
        let flexPill = RoutePill(route: busRoute, type: .flex)
        XCTAssertEqual(
            try flexPill.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "62/76"
        )
    }

    @MainActor func testHeavyRail() throws {
        let redLine = Route(
            id: .init("Red"),
            type: .heavyRail,
            color: "DA291C",
            directionNames: ["South", "North"],
            directionDestinations: ["Ashmont/Braintree", "Alewife"],
            isListedRoute: true,
            longName: "Red Line",
            shortName: "",
            sortOrder: 10010,
            textColor: "FFFFFF",
            lineId: .init("line-Red"),
            routePatternIds: nil
        )
        let blueLine = Route(
            id: .init("Blue"),
            type: .heavyRail,
            color: "003DA5",
            directionNames: ["West", "East"],
            directionDestinations: ["Bowdoin", "Wonderland"],
            isListedRoute: true,
            longName: "Blue Line",
            shortName: "",
            sortOrder: 10040,
            textColor: "FFFFFF",
            lineId: .init("line-Blue"),
            routePatternIds: nil
        )

        let redLineFixed = RoutePill(route: redLine, type: .fixed)
        let redLineFlex = RoutePill(route: redLine, type: .flex)
        let blueLineFixed = RoutePill(route: blueLine, type: .fixed)
        let blueLineFlex = RoutePill(route: blueLine, type: .flex)

        XCTAssertEqual(
            try redLineFixed.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "RL"
        )
        XCTAssertEqual(
            try redLineFlex.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "RL"
        )
        XCTAssertEqual(
            try blueLineFixed.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "BL"
        )
        XCTAssertEqual(
            try blueLineFlex.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "BL"
        )
    }

    @MainActor func testLightRail() throws {
        let greenLineC = Route(
            id: .init("Green-C"),
            type: .lightRail,
            color: "00843D",
            directionNames: ["West", "East"],
            directionDestinations: ["Cleveland Circle", "Government Center"],
            isListedRoute: true,
            longName: "Green Line C",
            shortName: "C",
            sortOrder: 10033,
            textColor: "FFFFFF",
            lineId: .init("line-Green"),
            routePatternIds: nil
        )

        let mattapan = Route(
            id: .init("Mattapan"),
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
            isListedRoute: true,
            longName: "Mattapan Trolley",
            shortName: "",
            sortOrder: 10011,
            textColor: "FFFFFF",
            lineId: .init("line-Mattapan"),
            routePatternIds: nil
        )

        let greenLineCFixed = RoutePill(route: greenLineC, type: .fixed)
        let greenLineCFlex = RoutePill(route: greenLineC, type: .flex)
        let mattapanFixed = RoutePill(route: mattapan, type: .fixed)
        let mattapanFlex = RoutePill(route: mattapan, type: .flex)

        XCTAssertEqual(
            try greenLineCFixed.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "GL C"
        )
        XCTAssertEqual(
            try greenLineCFlex.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "C"
        )
        XCTAssertEqual(
            try mattapanFixed.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "M"
        )
        XCTAssertEqual(
            try mattapanFlex.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "M"
        )
    }

    @MainActor func testCommuterRail() throws {
        let middleborough = Route(
            id: .init("CR-Middleborough"),
            type: .commuterRail,
            color: "80276C",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Middleborough/Lakeville", "South Station"],
            isListedRoute: true,
            longName: "Middleborough/Lakeville Line",
            shortName: "",
            sortOrder: 20009,
            textColor: "FFFFFF",
            lineId: .init("line-Middleborough"),
            routePatternIds: nil
        )
        let providence = Route(
            id: .init("CR-Providence"),
            type: .commuterRail,
            color: "80276C",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Stoughton or Wickford Junction", "South Station"],
            isListedRoute: true,
            longName: "Providence/Stoughton Line",
            shortName: "",
            sortOrder: 20012,
            textColor: "FFFFFF",
            lineId: .init("line-Providence"),
            routePatternIds: nil
        )

        let middleboroughFixed = RoutePill(route: middleborough, type: .fixed)
        let middleboroughFlex = RoutePill(route: middleborough, type: .flex)
        let providenceFixed = RoutePill(route: providence, type: .fixed)
        let providenceFlex = RoutePill(route: providence, type: .flex)

        XCTAssertEqual(
            try middleboroughFixed.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "CR"
        )
        XCTAssertEqual(
            try middleboroughFlex.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "Middleborough/Lakeville"
        )
        XCTAssertEqual(
            try providenceFixed.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "CR"
        )
        XCTAssertEqual(
            try providenceFlex.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "Providence/Stoughton"
        )
    }

    @MainActor func testFerry() throws {
        let ferry = Route(
            id: .init("Boat-F1"),
            type: .ferry,
            color: "008EAA",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Hingham or Hull", "Long Wharf or Rowes Wharf"],
            isListedRoute: true,
            longName: "Hingham/Hull Ferry",
            shortName: "",
            sortOrder: 30002,
            textColor: "FFFFFF",
            lineId: .init("line-Boat-F1"),
            routePatternIds: nil
        )

        let ferryFixed = RoutePill(route: ferry, type: .fixed)
        let ferryFlex = RoutePill(route: ferry, type: .flex)

        XCTAssertEqual(
            try ferryFixed.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().image().actualImage()
                .name(),
            "mode-ferry"
        )
        XCTAssertEqual(
            try ferryFlex.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "Hingham/Hull Ferry"
        )
    }

    @MainActor func testLines() throws {
        let redLine = Line(
            id: .init("line-Red"),
            color: "DA291C",
            longName: "Red Line",
            shortName: "",
            sortOrder: 10010,
            textColor: "FFFFFF"
        )

        let greenLine = Line(
            id: .init("line-Green"),
            color: "00843D",
            longName: "Green Line",
            shortName: "",
            sortOrder: 10032,
            textColor: "FFFFFF"
        )

        let rlFixed = RoutePill(route: nil, line: redLine, type: .fixed)
        let rlFlex = RoutePill(route: nil, line: redLine, type: .flex)
        let glFixed = RoutePill(route: nil, line: greenLine, type: .fixed)
        let glFlex = RoutePill(route: nil, line: greenLine, type: .flex)

        XCTAssertEqual(
            try rlFixed.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "Red Line"
        )
        XCTAssertEqual(
            try rlFlex.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "Red Line"
        )
        XCTAssertEqual(
            try glFixed.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "GL"
        )
        XCTAssertEqual(
            try glFlex.inspect().view(RoutePill.self).implicitAnyView().implicitAnyView().text().string(),
            "GL"
        )

        XCTAssertEqual(rlFixed.routeColor, Color(hex: "DA291C"))
        XCTAssertEqual(rlFlex.routeColor, Color(hex: "DA291C"))
        XCTAssertEqual(glFixed.routeColor, Color(hex: "00843D"))
        XCTAssertEqual(glFlex.routeColor, Color(hex: "00843D"))
    }
}

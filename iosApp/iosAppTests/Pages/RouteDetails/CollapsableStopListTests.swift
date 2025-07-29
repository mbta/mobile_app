//
//  CollapsableStopListTests.swift
//  iosAppTests
//
//  Created by Melody Horn on 7/8/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class CollapsableStopListTests: XCTestCase {
    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    func testWhenOneStopJustShowsThatStop() throws {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { stop in
            stop.name = "Stop 1"
            stop.locationType = .station
        }
        var clicked = false
        let mainRoute = objects.route { route in
            route.directionNames = ["West", "East"]
            route.directionDestinations = ["Here", "There"]
            route.longName = "Mauve Line"
            route.type = .heavyRail
        }

        let sut = CollapsableStopList(
            lineOrRoute: .route(mainRoute),
            segment: .init(
                stops: [.init(stop: stop1, stopLane: .center, stickConnections: [], connectingRoutes: [])],
                isTypical: false
            ),
            onClick: { _ in clicked = true },
            isFirstSegment: false,
            isLastSegment: false,
            rightSideContent: { _ in EmptyView() }
        ).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: stop1.name))
        XCTAssertNotNil(try sut.inspect().find(text: "Less common stop"))
        try sut.inspect().find(button: stop1.name).tap()
        XCTAssertTrue(clicked)
    }

    @MainActor func testWhenMultipleStopsCanExpandAndCollapse() {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { stop in
            stop.name = "Stop 1"
            stop.locationType = .station
        }
        let stop2 = objects.stop { stop in
            stop.name = "Stop 2"
            stop.locationType = .stop
            stop.vehicleType = .bus
        }
        let mainRoute = objects.route { route in
            route.directionNames = ["West", "East"]
            route.directionDestinations = ["Here", "There"]
            route.longName = "Mauve Line"
            route.type = .bus
        }

        let sut = CollapsableStopList(
            lineOrRoute: .route(mainRoute),
            segment: .init(
                stops: [.init(stop: stop1, stopLane: .center, stickConnections: [], connectingRoutes: []), .init(
                    stop: stop2,
                    stopLane: .center, stickConnections: [],
                    connectingRoutes: []
                )],
                isTypical: false
            ),
            onClick: { _ in },
            rightSideContent: { _ in EmptyView() }
        )

        let exp = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertFalse(try view.find(ViewType.DisclosureGroup.self).isExpanded())
            XCTAssertNotNil(try view.find(ViewType.DisclosureGroup.self).find(text: stop1.name))
            XCTAssertEqual(
                "2 less common stops",
                try view.find(ViewType.DisclosureGroup.self).labelView().find(ViewType.Text.self).string()
            )
            try view.find(ViewType.DisclosureGroup.self).expand()
            XCTAssertTrue(try view.find(ViewType.DisclosureGroup.self).isExpanded())
            XCTAssertNotNil(try view.find(text: stop2.name))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }
}

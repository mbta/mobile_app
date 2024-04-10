//
//  StopDetailsPageTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 4/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest
@_spi(Experimental) import MapboxMaps

final class StopDetailsPageTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testStopChange() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let nextStop = objects.stop { $0.id = "next" }
        let routePattern = objects.routePattern(route: route) { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))

        var sut = StopDetailsPage(
            backend: IdleBackend(),
            socket: MockSocket(),
            globalFetcher: .init(backend: IdleBackend()),
            viewportProvider: viewportProvider,
            stop: stop, filter: .init(routeId: route.id, directionId: routePattern.directionId)
        )

        let exp = sut.inspection.inspect { _ in
            XCTAssertEqual(viewportProvider.viewport.camera?.center, stop.coordinate)
            sut.stop = nextStop
            XCTAssertEqual(viewportProvider.viewport.camera?.center, nextStop.coordinate)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 2)
    }
}

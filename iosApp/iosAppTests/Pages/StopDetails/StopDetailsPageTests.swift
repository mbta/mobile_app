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
        let filter: Binding<StopDetailsFilter?> = .constant(.init(routeId: route.id, directionId: routePattern.directionId))

        var sut = StopDetailsPage(
            backend: IdleBackend(),
            socket: MockSocket(),
            globalFetcher: .init(backend: IdleBackend()),
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter
        )

        let exp = sut.inspection.inspect { _ in
            XCTAssertEqual(viewportProvider.viewport.camera?.center, stop.coordinate)
            sut.stop = nextStop
            XCTAssertEqual(viewportProvider.viewport.camera?.center, nextStop.coordinate)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 2)
    }

    func testClearsFilter() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let routePattern = objects.routePattern(route: route) { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let filter: Binding<StopDetailsFilter?> = .init(wrappedValue: .init(routeId: route.id, directionId: routePattern.directionId))

        let sut = StopDetailsPage(
            backend: IdleBackend(),
            socket: MockSocket(),
            globalFetcher: .init(backend: IdleBackend()),
            viewportProvider: viewportProvider,
            stop: stop,
            filter: filter
        )

        try sut.inspect().find(button: "Clear Filter").tap()
        XCTAssertNil(filter.wrappedValue)
    }
}

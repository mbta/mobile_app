//
//  HomeMapViewTest.swift
//  iosAppTests
//
//  Created by Simon, Emma on 4/10/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest
@_spi(Experimental) import MapboxMaps

final class HomeMapViewTest: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testAppears() throws {
        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let navStack: Binding<[SheetNavigationStackEntry]> = .init(wrappedValue: [])
        let sheetHeight: Binding<CGFloat> = .constant(100)

        var sut = HomeMapView(
            alertsFetcher: .init(socket: MockSocket()),
            globalFetcher: .init(backend: IdleBackend()),
            nearbyFetcher: .init(backend: IdleBackend()),
            railRouteShapeFetcher: .init(backend: IdleBackend()),
            viewportProvider: ViewportProvider(),
            navigationStack: navStack,
            sheetHeight: sheetHeight
        )

        let exp = sut.inspection.inspect { view in
            XCTAssertNotNil(view)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 2)
    }
}

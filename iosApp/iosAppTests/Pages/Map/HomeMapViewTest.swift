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
        let sheetHeight: Binding<CGFloat> = .constant(100)

        let sut = HomeMapView(
            globalFetcher: .init(backend: IdleBackend()),
            nearbyVM: .init(),
            railRouteShapeFetcher: .init(backend: IdleBackend()),
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: viewportProvider,
            sheetHeight: sheetHeight
        )

        let exp = sut.inspection.inspect { view in
            XCTAssertNotNil(view)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 2)
    }
}

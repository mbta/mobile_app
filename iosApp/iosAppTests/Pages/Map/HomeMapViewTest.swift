//
//  HomeMapViewTest.swift
//  iosAppTests
//
//  Created by Simon, Emma on 4/10/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
@_spi(Experimental) import MapboxMaps
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

final class HomeMapViewTest: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testAppears() throws {
        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let sheetHeight: Binding<CGFloat> = .constant(100)

        let sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(),
            nearbyVM: .init(),
            viewportProvider: viewportProvider,
            locationDataManager: .init(),
            sheetHeight: sheetHeight
        )

        let exp = sut.inspection.inspect { view in
            XCTAssertNotNil(view)
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 2)
    }
}

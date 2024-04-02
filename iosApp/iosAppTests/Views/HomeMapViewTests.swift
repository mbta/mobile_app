//
//  HomeMapViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import ViewInspector
@_spi(Experimental) import MapboxMaps
import shared
import XCTest

final class HomeMapViewTests: XCTestCase {
    struct NotUnderTestError: Error {}

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testNoLocationDefaultCenter() throws {
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend())
        let nearbyFetcher: NearbyFetcher = .init(backend: IdleBackend())
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyFetcher: nearbyFetcher,
            railRouteShapeFetcher: railRouteShapeFetcher,
            locationDataManager: locationDataManager,
            viewportProvider: ViewportProvider(),
            sheetHeight: .constant(0)
        )
        XCTAssertEqual(sut.viewportProvider.viewport.camera?.center, ViewportProvider.defaultCenter)
    }

    func testFollowsPuckWhenUserLocationIsKnown() throws {
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend())
        let nearbyFetcher: NearbyFetcher = .init(backend: IdleBackend())
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationFetcher = MockLocationFetcher()
        locationFetcher.authorizationStatus = .authorizedAlways

        let locationDataManager: LocationDataManager = .init(locationFetcher: locationFetcher)
        let newLocation: CLLocation = .init(latitude: 42, longitude: -71)

        var sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyFetcher: nearbyFetcher,
            railRouteShapeFetcher: railRouteShapeFetcher,
            locationDataManager: locationDataManager,
            viewportProvider: ViewportProvider(),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { _ in
            XCTAssertNotNil(sut.viewportProvider.viewport.followPuck)
        }
        ViewHosting.host(view: sut)
        locationFetcher.updateLocations(locations: [newLocation])
        XCTAssertEqual(locationDataManager.currentLocation, newLocation)

        wait(for: [hasAppeared], timeout: 5)
    }

    func testFetchData() throws {
        class FakeGlobalFetcher: GlobalFetcher {
            init() {
                super.init(backend: IdleBackend())
            }

            override func getGlobalData() async throws {
                XCTFail("Map tried to fetch global data")
                throw NotUnderTestError()
            }
        }

        class FakeRailRouteShapeFetcher: RailRouteShapeFetcher {
            let getRailRouteShapeExpectation: XCTestExpectation

            init(getRailRouteShapeExpectation: XCTestExpectation) {
                self.getRailRouteShapeExpectation = getRailRouteShapeExpectation
                super.init(backend: IdleBackend())
            }

            override func getRailRouteShapes() async throws {
                getRailRouteShapeExpectation.fulfill()
                throw NotUnderTestError()
            }
        }

        let getRailRouteShapeExpectation = expectation(description: "getRailRouteShapes")

        var sut = HomeMapView(
            globalFetcher: FakeGlobalFetcher(),
            nearbyFetcher: NearbyFetcher(backend: IdleBackend()),
            railRouteShapeFetcher: FakeRailRouteShapeFetcher(getRailRouteShapeExpectation: getRailRouteShapeExpectation),
            viewportProvider: ViewportProvider(),
            sheetHeight: .constant(0)
        )
        let hasAppeared = sut.on(\.didAppear) { _ in }
        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 5)
        wait(for: [getRailRouteShapeExpectation], timeout: 1)
    }
}

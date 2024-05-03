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
import SwiftUI
import XCTest

final class HomeMapViewTests: XCTestCase {
    struct NotUnderTestError: Error {}

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testNoLocationDefaultCenter() throws {
        let alertsFetcher: AlertsFetcher = .init(socket: MockSocket())
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend())
        let nearbyFetcher: NearbyFetcher = .init(backend: IdleBackend())
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(
            alertsFetcher: alertsFetcher,
            globalFetcher: globalFetcher,
            nearbyFetcher: nearbyFetcher,
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            navigationStack: .constant([]),
            sheetHeight: .constant(0)
        )
        XCTAssertEqual(sut.viewportProvider.viewport.camera?.center, ViewportProvider.Defaults.center)
    }

    func testFollowsPuckWhenUserLocationIsKnown() throws {
        let alertsFetcher: AlertsFetcher = .init(socket: MockSocket())
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend())
        let nearbyFetcher: NearbyFetcher = .init(backend: IdleBackend())
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationFetcher = MockLocationFetcher()
        locationFetcher.authorizationStatus = .authorizedAlways

        let locationDataManager: LocationDataManager = .init(locationFetcher: locationFetcher)
        let newLocation: CLLocation = .init(latitude: 42, longitude: -71)

        var sut = HomeMapView(
            alertsFetcher: alertsFetcher,
            globalFetcher: globalFetcher,
            nearbyFetcher: nearbyFetcher,
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            navigationStack: .constant([]),
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
        let alertsFetcher: AlertsFetcher = .init(socket: MockSocket())
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
            alertsFetcher: alertsFetcher,
            globalFetcher: FakeGlobalFetcher(),
            nearbyFetcher: NearbyFetcher(backend: IdleBackend()),
            railRouteShapeFetcher: FakeRailRouteShapeFetcher(getRailRouteShapeExpectation: getRailRouteShapeExpectation),
            vehiclesFetcher: VehiclesFetcher(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            navigationStack: .constant([]),
            sheetHeight: .constant(0)
        )
        let hasAppeared = sut.on(\.didAppear) { _ in }
        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 5)
        wait(for: [getRailRouteShapeExpectation], timeout: 1)
    }

    func testCentersOnSelectedStop() throws {
        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }
        let alertsFetcher: AlertsFetcher = .init(socket: MockSocket())
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [stop.id: stop], routes: [:])
        let nearbyFetcher: NearbyFetcher = .init(backend: IdleBackend())
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            alertsFetcher: alertsFetcher,
            globalFetcher: globalFetcher,
            nearbyFetcher: nearbyFetcher,
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            navigationStack: .constant([.stopDetails(stop, nil)]),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { _ in }
        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 1)
        XCTAssertEqual(stop.coordinate, sut.viewportProvider.viewport.camera!.center)
    }

    func testCentersOnStopWhenNewSelectedStop() throws {
        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }
        let alertsFetcher: AlertsFetcher = .init(socket: MockSocket())
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [stop.id: stop], routes: [:])
        let nearbyFetcher: NearbyFetcher = .init(backend: IdleBackend())
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            alertsFetcher: alertsFetcher,
            globalFetcher: globalFetcher,
            nearbyFetcher: nearbyFetcher,
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            navigationStack: .constant([]),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { _ in }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 1)
        XCTAssertEqual(ViewportProvider.Defaults.center, sut.viewportProvider.viewport.camera!.center)

        let newEntry: SheetNavigationStackEntry = .stopDetails(stop, nil)

        try sut.inspect().find(ProxyModifiedMap.self).callOnChange(newValue: newEntry)
        XCTAssertEqual(stop.coordinate, sut.viewportProvider.viewport.camera!.center)
    }

    class FakeLayerManager: IMapLayerManager {
        var routeSourceGenerator: RouteSourceGenerator?
        var routeLayerGenerator: RouteLayerGenerator?
        var stopSourceGenerator: StopSourceGenerator?
        var stopLayerGenerator: StopLayerGenerator?
        private let updateRouteSourceCallback: (RouteSourceGenerator) -> Void

        init(updateRouteSourceCallback: @escaping (RouteSourceGenerator) -> Void) {
            self.updateRouteSourceCallback = updateRouteSourceCallback
        }

        func addSources(routeSourceGenerator _: RouteSourceGenerator, stopSourceGenerator _: StopSourceGenerator) {}
        func addLayers(routeLayerGenerator _: RouteLayerGenerator, stopLayerGenerator _: StopLayerGenerator) {}
        func updateSourceData(routeSourceGenerator: RouteSourceGenerator, stopSourceGenerator _: StopSourceGenerator) {
            updateRouteSourceCallback(routeSourceGenerator)
        }

        func updateSourceData(routeSourceGenerator: RouteSourceGenerator) {
            updateRouteSourceCallback(routeSourceGenerator)
        }

        func updateSourceData(stopSourceGenerator _: StopSourceGenerator) {}

        func updateStopLayerZoom(_: CGFloat) {}
    }

    func testUpdatesRouteSourceWhenStopSelected() throws {
        class OLOnlyStopRepository: IStopRepository {
            func __getStopMapData(stopId _: String) async throws -> StopMapResponse {
                StopMapResponse(routeShapes: MapTestDataHelper.routeResponse.routesWithSegmentedShapes.filter { $0.routeId == MapTestDataHelper.routeOrange.id }, childStops: [:])
            }
        }
        HelpersKt.loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(stop: OLOnlyStopRepository()))

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }

        let olRouteSourceUpdateExpectation = XCTestExpectation(description: "updateRouteSource called only OL route")
        func olOnlyRouteSourceCheck(routeGenerator: RouteSourceGenerator) {
            if routeGenerator.routeLines.allSatisfy { $0.routeId == MapTestDataHelper.routeOrange.id } {
                olRouteSourceUpdateExpectation.fulfill()
            }
        }
        let alertsFetcher: AlertsFetcher = .init(socket: MockSocket())
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [stop.id: stop], routes: [:])
        let nearbyFetcher: NearbyFetcher = .init(backend: IdleBackend())
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            alertsFetcher: alertsFetcher,
            globalFetcher: globalFetcher,
            nearbyFetcher: nearbyFetcher,
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            navigationStack: .constant([]),
            sheetHeight: .constant(0),
            layerManager: FakeLayerManager(updateRouteSourceCallback: olOnlyRouteSourceCheck)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            let newNavStackEntry: SheetNavigationStackEntry = .stopDetails(stop, nil)
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: newNavStackEntry)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, olRouteSourceUpdateExpectation], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testShowsAllRailShapesWhenSelectedStopCleared() throws {
        class OLOnlyStopRepository: IStopRepository {
            func __getStopMapData(stopId _: String) async throws -> StopMapResponse {
                StopMapResponse(routeShapes: MapTestDataHelper.routeResponse.routesWithSegmentedShapes.filter { $0.routeId == MapTestDataHelper.routeOrange.id }, childStops: [:])
            }
        }

        HelpersKt.loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(stop: OLOnlyStopRepository()))

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }

        let allRailRouteSourceUpdateExpectation = XCTestExpectation(description: "updateRouteSource called with all rail sources")
        func allRailRouteSourceCheck(routeGenerator: RouteSourceGenerator) {
            if routeGenerator.routeData == MapTestDataHelper.routeResponse.routesWithSegmentedShapes {
                allRailRouteSourceUpdateExpectation.fulfill()
            }
        }
        let alertsFetcher: AlertsFetcher = .init(socket: MockSocket())
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [stop.id: stop], routes: [:])
        let nearbyFetcher: NearbyFetcher = .init(backend: IdleBackend())
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            alertsFetcher: alertsFetcher,
            globalFetcher: globalFetcher,
            nearbyFetcher: nearbyFetcher,
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            navigationStack: .constant([.stopDetails(stop, nil)]),
            sheetHeight: .constant(0),
            layerManager: FakeLayerManager(updateRouteSourceCallback: allRailRouteSourceCheck)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: nil as SheetNavigationStackEntry?)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, allRailRouteSourceUpdateExpectation], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }
}

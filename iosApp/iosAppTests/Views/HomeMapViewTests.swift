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

// swiftlint:disable:next type_body_length
final class HomeMapViewTests: XCTestCase {
    struct NotUnderTestError: Error {}

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testNoLocationDefaultCenter() throws {
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend())
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )
        XCTAssertEqual(sut.viewportProvider.viewport.camera?.center, ViewportProvider.Defaults.center)
    }

    func testFollowsPuckWhenUserLocationIsKnown() throws {
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend())
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationFetcher = MockLocationFetcher()
        locationFetcher.authorizationStatus = .authorizedAlways

        let locationDataManager: LocationDataManager = .init(locationFetcher: locationFetcher)
        let newLocation: CLLocation = .init(latitude: 42, longitude: -71)

        var sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
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
            nearbyVM: .init(),
            railRouteShapeFetcher: FakeRailRouteShapeFetcher(
                getRailRouteShapeExpectation: getRailRouteShapeExpectation
            ),
            vehiclesFetcher: VehiclesFetcher(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
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
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [stop.id: stop], routes: [:])
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyVM: .init(navigationStack: [.stopDetails(stop, nil)]),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
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
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [stop.id: stop], routes: [:])
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
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
    }

    func testUpdatesRouteSourceWhenStopSelected() throws {
        class OLOnlyStopRepository: IStopRepository {
            func __getStopMapData(stopId _: String) async throws -> StopMapResponse {
                StopMapResponse(
                    routeShapes: MapTestDataHelper.routeResponse.routesWithSegmentedShapes
                        .filter { $0.routeId == MapTestDataHelper.routeOrange.id },
                    childStops: [:]
                )
            }
        }
        HelpersKt
            .loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(stop: OLOnlyStopRepository()))

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }

        let olRouteSourceUpdateExpectation = XCTestExpectation(description: "updateRouteSource called only OL route")
        func olOnlyRouteSourceCheck(routeGenerator: RouteSourceGenerator) {
            if routeGenerator.routeLines.allSatisfy({ $0.routeId == MapTestDataHelper.routeOrange.id }) {
                olRouteSourceUpdateExpectation.fulfill()
            }
        }
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [stop.id: stop], routes: [:])
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
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

    func testUpdatesRouteSourceWhenStopSelectedWithRouteFilter() throws {
        class FakeStopRepository: IStopRepository {
            func __getStopMapData(stopId _: String) async throws -> StopMapResponse {
                StopMapResponse(routeShapes: MapTestDataHelper.routeResponse.routesWithSegmentedShapes, childStops: [:])
            }
        }
        HelpersKt.loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(stop: FakeStopRepository()))

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }

        let olRouteSourceUpdateExpectation = XCTestExpectation(description: "updateRouteSource called only OL route")
        func olOnlyRouteSourceCheck(routeGenerator: RouteSourceGenerator) {
            if routeGenerator.routeLines.allSatisfy({ $0.routeId == MapTestDataHelper.routeOrange.id }) {
                olRouteSourceUpdateExpectation.fulfill()
            }
        }
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [stop.id: stop], routes: [:])
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0),
            layerManager: FakeLayerManager(updateRouteSourceCallback: olOnlyRouteSourceCheck)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            let newNavStackEntry: SheetNavigationStackEntry =
                .stopDetails(stop, .init(routeId: MapTestDataHelper.routeOrange.id,
                                         directionId: MapTestDataHelper.patternOrange30.directionId))
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: newNavStackEntry)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, olRouteSourceUpdateExpectation], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testUpdatesRouteSourceWhenStopSelectedWithRouteFilterAndUpcomingDepartures() throws {
        class FakeStopRepository: IStopRepository {
            func __getStopMapData(stopId _: String) async throws -> StopMapResponse {
                StopMapResponse(routeShapes: MapTestDataHelper.routeResponse.routesWithSegmentedShapes, childStops: [:])
            }
        }
        HelpersKt.loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(stop: FakeStopRepository()))

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }
        let trip = objectCollection.trip { trip in
            trip.routePatternId = MapTestDataHelper.patternOrange30.id
        }

        let prediction = objectCollection.prediction { prediction in
            prediction.trip = trip
        }

        let olRouteSourceUpdateExpectation = XCTestExpectation(description: "updateRouteSouce called for expected RP")
        func olOnlyRouteSourceCheck(routeGenerator: RouteSourceGenerator) {
            if routeGenerator.routeLines.allSatisfy({ $0.routePatternId == MapTestDataHelper.patternOrange30.id }) {
                olRouteSourceUpdateExpectation.fulfill()
            }
        }

        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [stop.id: stop], routes: [:])
        let nearbyVM: NearbyViewModel = .init()
        nearbyVM.setDepartures(StopDetailsDepartures(routes:
            [.init(route: MapTestDataHelper.routeOrange, stop: stop,
                   patternsByHeadsign: [.init(route: MapTestDataHelper.routeOrange,
                                              headsign: MapTestDataHelper.tripOrangeC1.headsign,
                                              patterns: [MapTestDataHelper.patternOrange30],
                                              upcomingTrips: [UpcomingTrip(trip: trip, prediction: prediction)],
                                              alertsHere: nil)])]))

        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0),
            layerManager: FakeLayerManager(updateRouteSourceCallback: olOnlyRouteSourceCheck)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            let newNavStackEntry: SheetNavigationStackEntry =
                .stopDetails(stop, .init(routeId: MapTestDataHelper.routeOrange.id,
                                         directionId: MapTestDataHelper.patternOrange30.directionId))
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: newNavStackEntry)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, olRouteSourceUpdateExpectation], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testVehicleTapping() throws {
        class FakeStopRepository: IStopRepository {
            func __getStopMapData(stopId _: String) async throws -> StopMapResponse {
                StopMapResponse(routeShapes: MapTestDataHelper.routeResponse.routesWithSegmentedShapes, childStops: [:])
            }
        }
        HelpersKt.loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(stop: FakeStopRepository()))

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }
        let trip = objectCollection.trip { trip in
            trip.routePatternId = MapTestDataHelper.patternOrange30.id
            trip.id = "1"
            trip.directionId = 0
        }

        let prediction = objectCollection.prediction { prediction in
            prediction.trip = trip
            prediction.stopSequence = 100
        }

        let vehicle = objectCollection.vehicle { vehicle in
            vehicle.id = "1"
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = trip.id
            vehicle.routeId = MapTestDataHelper.patternOrange30.routeId
            vehicle.directionId = 0
        }

        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [stop.id: stop], routes: [:])
        let nearbyVM: NearbyViewModel = .init()
        nearbyVM.setDepartures(StopDetailsDepartures(routes:
            [.init(route: MapTestDataHelper.routeOrange, stop: stop,
                   patternsByHeadsign: [.init(route: MapTestDataHelper.routeOrange,
                                              headsign: MapTestDataHelper.tripOrangeC1.headsign,
                                              patterns: [MapTestDataHelper.patternOrange30],
                                              upcomingTrips: [UpcomingTrip(trip: trip, prediction: prediction)],
                                              alertsHere: nil)])]))

        let initialNav: SheetNavigationStackEntry = .stopDetails(
            stop,
            .init(routeId: vehicle.routeId!, directionId: vehicle.directionId)
        )
        nearbyVM.navigationStack = [initialNav]
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyVM: nearbyVM,
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket(), vehicles: [vehicle]),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            XCTAssertEqual(nearbyVM.navigationStack.last, initialNav)
            try sut.find(HomeMapView.self).actualView().handleTapVehicle(vehicle)
            XCTAssertEqual(nearbyVM.navigationStack.last, .tripDetails(
                tripId: trip.id,
                vehicleId: vehicle.id,
                target: .init(stopId: stop.id, stopSequence: Int(prediction.stopSequence))
            ))
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testShowsAllRailShapesWhenSelectedStopCleared() throws {
        class OLOnlyStopRepository: IStopRepository {
            func __getStopMapData(stopId _: String) async throws -> StopMapResponse {
                StopMapResponse(
                    routeShapes: MapTestDataHelper.routeResponse.routesWithSegmentedShapes
                        .filter { $0.routeId == MapTestDataHelper.routeOrange.id },
                    childStops: [:]
                )
            }
        }

        HelpersKt
            .loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(stop: OLOnlyStopRepository()))

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }

        let allRailRouteSourceUpdateExpectation =
            XCTestExpectation(description: "updateRouteSource called with all rail sources")
        func allRailRouteSourceCheck(routeGenerator: RouteSourceGenerator) {
            if routeGenerator.routeData == MapTestDataHelper.routeResponse.routesWithSegmentedShapes {
                allRailRouteSourceUpdateExpectation.fulfill()
            }
        }
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [stop.id: stop], routes: [:])
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyVM: .init(navigationStack: [.stopDetails(stop, nil)]),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
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

    func testUpdatesViewportOnCameraChangeBeforeLayersLoad() throws {
        let updateCameraExpectation = XCTestExpectation(description: "updateCameraState called")

        class FakeViewportProvider: ViewportProvider {
            let updateCameraExpectation: XCTestExpectation

            init(updateCameraExpectation: XCTestExpectation) {
                self.updateCameraExpectation = updateCameraExpectation
            }

            override func updateCameraState(_: CameraState) {
                updateCameraExpectation.fulfill()
            }
        }
        let globalFetcher: GlobalFetcher = .init(backend: IdleBackend(), stops: [:], routes: [:])
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let viewportProvider: ViewportProvider = FakeViewportProvider(updateCameraExpectation: updateCameraExpectation)
        let sut = HomeMapView(
            globalFetcher: globalFetcher,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: viewportProvider,
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0),
            layerManager: nil
        )

        sut.handleCameraChange(.init(cameraState: CameraState(center: .init(latitude: 2, longitude: 2),
                                                              padding: .zero,
                                                              zoom: 12,
                                                              bearing: .leastNormalMagnitude,
                                                              pitch: 0.0), timestamp: Date.now))

        wait(for: [updateCameraExpectation], timeout: 5)
    }
}

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
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(
            mapVM: .init(),
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
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationFetcher = MockLocationFetcher()
        locationFetcher.authorizationStatus = .authorizedAlways

        let locationDataManager: LocationDataManager = .init(locationFetcher: locationFetcher)
        let newLocation: CLLocation = .init(latitude: 42, longitude: -71)

        var sut = HomeMapView(
            mapVM: .init(),
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
            mapVM: .init(),
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
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: .init(),
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
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: .init(),
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
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: .init(),
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
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: .init(),
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

        let nearbyVM: NearbyViewModel = .init()
        nearbyVM.setDepartures(StopDetailsDepartures(routes:
            [.init(route: MapTestDataHelper.routeOrange, stop: stop,
                   patterns: [.ByHeadsign(
                       route: MapTestDataHelper.routeOrange,
                       headsign: MapTestDataHelper.tripOrangeC1.headsign,
                       line: nil,
                       patterns: [MapTestDataHelper.patternOrange30],
                       upcomingTrips: [UpcomingTrip(trip: trip, prediction: prediction)],
                       alertsHere: nil
                   )])]))

        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: .init(),
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

    func testUpdatesRouteSourceWhenTripSelected() throws {
        class FakeTripRepository: IdleTripRepository {
            override func __getTripShape(tripId _: String) async throws -> ApiResult<TripShape> {
                ApiResultOk(data: .init(shapeWithStops: .init(directionId: 1,
                                                              routeId: MapTestDataHelper.routeOrange.id,
                                                              routePatternId: MapTestDataHelper.patternOrange30
                                                                  .id,
                                                              shape: MapTestDataHelper.shapeOrangeC1,
                                                              stopIds: [MapTestDataHelper.stopAssembly.id,
                                                                        MapTestDataHelper.stopSullivan.id])))
            }
        }
        HelpersKt
            .loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(trip: FakeTripRepository()))

        let olRouteSourceUpdateExpectation = XCTestExpectation(description: "updateRouteSource called with trip shape")
        func olOnlyRouteSourceCheck(routeGenerator: RouteSourceGenerator) {
            if routeGenerator.routeLines.allSatisfy({ $0.routePatternId == MapTestDataHelper.patternOrange30.id }) {
                olRouteSourceUpdateExpectation.fulfill()
            }
        }
        let globalRepo: IGlobalRepository = MockGlobalRepository(response: .init(
            lines: [:],
            patternIdsByStop: [:],
            routes: [
                MapTestDataHelper.routeOrange.id: MapTestDataHelper.routeOrange,
            ],
            routePatterns: [:],
            stops: [MapTestDataHelper.stopAssembly.id: MapTestDataHelper.stopAssembly,
                    MapTestDataHelper.stopSullivan.id: MapTestDataHelper.stopSullivan],
            trips: [:]
        ))
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            globalRepository: globalRepo,
            mapVM: .init(),
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0),
            layerManager: FakeLayerManager(updateRouteSourceCallback: olOnlyRouteSourceCheck)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            let newNavStackEntry: SheetNavigationStackEntry = .tripDetails(tripId: "ol_trip_id",
                                                                           vehicleId: "vehicle",
                                                                           target: nil,
                                                                           routeId: MapTestDataHelper.routeOrange.id,
                                                                           directionId: MapTestDataHelper
                                                                               .patternOrange30
                                                                               .directionId)
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

        let nearbyVM: NearbyViewModel = .init()
        nearbyVM.setDepartures(StopDetailsDepartures(routes:
            [.init(route: MapTestDataHelper.routeOrange, stop: stop,
                   patterns: [.ByHeadsign(route: MapTestDataHelper.routeOrange,
                                          headsign: MapTestDataHelper.tripOrangeC1.headsign,
                                          line: nil,
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
            mapVM: .init(),
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
                target: .init(stopId: stop.id, stopSequence: Int(prediction.stopSequence)),
                routeId: trip.routeId,
                directionId: trip.directionId
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
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: .init(),
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
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let viewportProvider: ViewportProvider = FakeViewportProvider(updateCameraExpectation: updateCameraExpectation)
        let sut = HomeMapView(
            mapVM: .init(),
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

    func testLayersRestoredOnActive() throws {
        let addLayersCalledExpectation = XCTestExpectation(description: "Add layers called")
        let updateSourcesCalledExpectation = XCTestExpectation(description: "Update layers called")

        var sut = HomeMapView(
            mapVM: .init(),
            nearbyVM: .init(),
            railRouteShapeFetcher: .init(backend: IdleBackend()),
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: .init(),
            sheetHeight: .constant(0),
            layerManager: FakeLayerManager(addLayersCallback: { addLayersCalledExpectation.fulfill() },
                                           updateRouteSourceCallback: { _ in updateSourcesCalledExpectation.fulfill() })
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: ScenePhase.active)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, addLayersCalledExpectation, updateSourcesCalledExpectation], timeout: 5)
    }

    func testLayersNotReInitWhenAlertsChanges() throws {
        let addLayersNotCalledExpectation = XCTestExpectation(description: "Add layers not called")
        addLayersNotCalledExpectation.isInverted = true
        let updateSourcesCalledExpectation = XCTestExpectation(description: "Update layers called")

        let layerManager = FakeLayerManager(addLayersCallback: { addLayersNotCalledExpectation.fulfill() },
                                            updateRouteSourceCallback: { _ in
                                                updateSourcesCalledExpectation.fulfill()
                                            })
        var sut = HomeMapView(
            mapVM: .init(),
            nearbyVM: .init(),
            railRouteShapeFetcher: .init(backend: IdleBackend()),
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: .init(),
            sheetHeight: .constant(0),
            layerManager: layerManager
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: GlobalMapData(mapStops: [:], alertsByStop: [:]))
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, addLayersNotCalledExpectation, updateSourcesCalledExpectation], timeout: 5)
    }

    class FakeLayerManager: IMapLayerManager {
        var routeSourceGenerator: RouteSourceGenerator?
        var routeLayerGenerator: RouteLayerGenerator?
        var stopSourceGenerator: StopSourceGenerator?
        var stopLayerGenerator: StopLayerGenerator?
        var childStopSourceGenerator: ChildStopSourceGenerator?
        var childStopLayerGenerator: ChildStopLayerGenerator?
        private let addLayersCallback: () -> Void
        private let updateRouteSourceCallback: (RouteSourceGenerator) -> Void

        init(addLayersCallback: @escaping () -> Void = {},
             updateRouteSourceCallback: @escaping (RouteSourceGenerator) -> Void = { _ in }) {
            self.updateRouteSourceCallback = updateRouteSourceCallback
            self.addLayersCallback = addLayersCallback
        }

        func addSources(
            routeSourceGenerator _: RouteSourceGenerator,
            stopSourceGenerator _: StopSourceGenerator,
            childStopSourceGenerator _: ChildStopSourceGenerator
        ) {}
        func addLayers(
            routeLayerGenerator _: RouteLayerGenerator,
            stopLayerGenerator _: StopLayerGenerator,
            childStopLayerGenerator _: ChildStopLayerGenerator
        ) {
            addLayersCallback()
        }

        func updateSourceData(
            routeSourceGenerator: RouteSourceGenerator,
            stopSourceGenerator _: StopSourceGenerator,
            childStopSourceGenerator _: ChildStopSourceGenerator
        ) {
            updateRouteSourceCallback(routeSourceGenerator)
        }

        func updateSourceData(routeSourceGenerator: RouteSourceGenerator) {
            updateRouteSourceCallback(routeSourceGenerator)
        }

        func updateSourceData(stopSourceGenerator _: StopSourceGenerator) {}
        func updateSourceData(childStopSourceGenerator _: ChildStopSourceGenerator) {}
    }
}

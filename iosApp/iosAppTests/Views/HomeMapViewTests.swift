//
//  HomeMapViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
@_spi(Experimental) import MapboxMaps
import Shared
import SwiftUI
import ViewInspector
import XCTest

// swiftlint:disable:next type_body_length
final class HomeMapViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    class FilteredStopRepository: IStopRepository {
        private var onGetStopMapData: () -> Void
        private var filteredRouteIds: Set<String>?

        init(filteredRouteIds: Set<String>? = nil, onGetStopMapData: @escaping () -> Void = {}) {
            self.onGetStopMapData = onGetStopMapData
            self.filteredRouteIds = filteredRouteIds
        }

        func __getStopMapData(stopId _: String) async throws -> ApiResult<StopMapResponse> {
            onGetStopMapData()

            return ApiResultOk(data: StopMapResponse(
                routeShapes: MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes
                    .filter { if let filteredRouteIds = self.filteredRouteIds {
                        filteredRouteIds.contains($0.routeId)
                    } else {
                        true
                    }
                    },
                childStops: [:]
            ))
        }
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
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    func testNoLocationDefaultCenter() throws {
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(),
            nearbyVM: .init(),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )
        XCTAssertEqual(sut.viewportProvider.viewport.camera?.center, ViewportProvider.Defaults.center)
    }

    // Test is ignored in the test plan
    func testFollowsPuckWhenUserLocationIsKnown() throws {
        let locationFetcher = MockLocationFetcher()
        locationFetcher.authorizationStatus = .authorizedAlways

        let locationDataManager: LocationDataManager = .init(locationFetcher: locationFetcher)
        let newLocation: CLLocation = .init(latitude: 42, longitude: -71)

        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(),
            nearbyVM: .init(),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { _ in
            XCTAssertNotNil(sut.viewportProvider.viewport.followPuck)
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        locationFetcher.updateLocations(locations: [newLocation])
        XCTAssertEqual(locationDataManager.currentLocation, newLocation)

        wait(for: [hasAppeared], timeout: 5)
    }

    func testFetchData() throws {
        let getRailRouteShapeExpectation = expectation(description: "getRailRouteShapes")
        let railRouteShapeRepository = MockRailRouteShapeRepository(onGet: { getRailRouteShapeExpectation.fulfill() })

        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(),
            nearbyVM: .init(),
            viewportProvider: ViewportProvider(),
            railRouteShapeRepository: railRouteShapeRepository,
            locationDataManager: .init(),
            sheetHeight: .constant(0)
        )
        let hasAppeared = sut.on(\.didAppear) { _ in }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared], timeout: 5)
        wait(for: [getRailRouteShapeExpectation], timeout: 1)
    }

    func testCentersOnStopWhenNewSelectedStop() throws {
        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(globalData: .init(objects: objectCollection)),
            nearbyVM: .init(),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { view in
            XCTAssertEqual(
                ViewportProvider.Defaults.center,
                try view.actualView().viewportProvider.viewport.camera!.center
            )
            XCTAssertEqual(ViewportProvider.Defaults.zoom, try view.actualView().viewportProvider.viewport.camera!.zoom)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared], timeout: 1)

        let newZoom = 10.0
        sut.viewportProvider.updateCameraState(
            .init(center: .init(latitude: 0, longitude: 0), padding: .zero, zoom: newZoom, bearing: 0, pitch: 0)
        )

        let newEntry: SheetNavigationStackEntry = .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)

        try sut.inspect().find(ProxyModifiedMap.self).findAndCallOnChange(relation: .parent, newValue: newEntry)
        XCTAssertEqual(stop.coordinate, sut.viewportProvider.viewport.camera!.center)
        XCTAssertEqual(newZoom, sut.viewportProvider.viewport.camera!.zoom)
    }

    func testUpdatesStopLayersWhenStopSelected() throws {
        let repositories = MockRepositories()
        repositories.stop = FilteredStopRepository(filteredRouteIds: [MapTestDataHelper.shared.routeOrange.id])
        HelpersKt.loadKoinMocks(repositories: repositories)

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }

        let mapVM: MapViewModel = .init(layerManager: MockLayerManager(), globalData: .init(objects: objectCollection))

        let railRouteShapeRepository = MockRailRouteShapeRepository(response: MapTestDataHelper.shared.routeResponse)
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: mapVM,
            nearbyVM: .init(),
            viewportProvider: ViewportProvider(),
            railRouteShapeRepository: railRouteShapeRepository,
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            XCTAssertEqual(mapVM.stopLayerState, .init())
            let newNavStackEntry: SheetNavigationStackEntry =
                .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(relation: .parent, newValue: newNavStackEntry)
            XCTAssertEqual(mapVM.stopLayerState, .init(selectedStopId: stop.id))
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    @MainActor func testUpdatesRouteAndChildStopsWhenStopSelected() throws {
        let globalLoadSubject = PassthroughSubject<Void, Never>()

        let objects = MapTestDataHelper.shared.objects
        let stop = objects.stop { stop in
            stop.latitude = 1
            stop.longitude = 1
        }

        let stopMapDetailsLoadedPublisher = PassthroughSubject<Void, Never>()
        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(
            response: .init(objects: objects),
            onGet: { globalLoadSubject.send() }
        )
        repositories.stop = FilteredStopRepository(
            filteredRouteIds: [MapTestDataHelper.shared.routeOrange.id],
            onGetStopMapData: { stopMapDetailsLoadedPublisher.send() }
        )
        HelpersKt.loadKoinMocks(repositories: repositories)

        let mapVM: MapViewModel = .init(layerManager: MockLayerManager())
        mapVM.allRailSourceData = MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes

        let railRouteShapeRepository = MockRailRouteShapeRepository(response: MapTestDataHelper.shared.routeResponse)
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(
            contentVM: .init(),
            mapVM: mapVM,
            nearbyVM: .init(),
            viewportProvider: ViewportProvider(),
            railRouteShapeRepository: railRouteShapeRepository,
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.inspection.inspect(onReceive: globalLoadSubject, after: 1) { sut in
            let newNavStackEntry: SheetNavigationStackEntry =
                .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(relation: .parent, newValue: newNavStackEntry)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        let stopRelatedDataSet = sut.inspection.inspect(onReceive: stopMapDetailsLoadedPublisher, after: 1) { _ in
            XCTAssertFalse(mapVM.routeSourceData.isEmpty)
            XCTAssertTrue(mapVM.routeSourceData.allSatisfy { $0.routeId == MapTestDataHelper.shared.routeOrange.id })
        }
        wait(for: [hasAppeared, stopRelatedDataSet], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    @MainActor func testSetsRouteSourceWhenStopSelectedWithRouteFilter() throws {
        let globalLoadSubject = PassthroughSubject<Void, Never>()
        let stopMapDetailsLoadedPublisher = PassthroughSubject<Void, Never>()

        let objects = MapTestDataHelper.shared.objects
        let stop = objects.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }

        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(
            response: .init(objects: objects),
            onGet: { globalLoadSubject.send() }
        )
        repositories.stop = FilteredStopRepository(
            filteredRouteIds: [MapTestDataHelper.shared.routeOrange.id],
            onGetStopMapData: { stopMapDetailsLoadedPublisher.send() }
        )
        HelpersKt.loadKoinMocks(repositories: repositories)

        let mapVM: MapViewModel = .init(layerManager: MockLayerManager())
        mapVM.allRailSourceData = MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes

        let railRouteShapeRepository = MockRailRouteShapeRepository(response: MapTestDataHelper.shared.routeResponse)

        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(
            contentVM: .init(),
            mapVM: mapVM,
            nearbyVM: .init(),
            viewportProvider: ViewportProvider(),
            railRouteShapeRepository: railRouteShapeRepository,
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.inspection.inspect(onReceive: globalLoadSubject, after: 1) { sut in
            let newNavStackEntry: SheetNavigationStackEntry =
                .stopDetails(
                    stopId: stop.id,
                    stopFilter: .init(
                        routeId: MapTestDataHelper.shared.routeOrange.id,
                        directionId: MapTestDataHelper.shared.patternOrange30.directionId
                    ),
                    tripFilter: nil
                )
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(relation: .parent, newValue: newNavStackEntry)
        }

        let stopRelatedDataSet = sut.inspection.inspect(onReceive: stopMapDetailsLoadedPublisher, after: 1) { _ in
            XCTAssertFalse(mapVM.routeSourceData.isEmpty)
            XCTAssertTrue(mapVM.routeSourceData.allSatisfy { $0.routeId == MapTestDataHelper.shared.routeOrange.id })
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, stopRelatedDataSet], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    @MainActor func testUpdatesRouteSourceWhenStopSelectedWithRouteFilterAndUpcomingDepartures() throws {
        let globalLoadSubject = PassthroughSubject<Void, Never>()
        let stopMapDetailsLoadedPublisher = PassthroughSubject<Void, Never>()

        let objects = MapTestDataHelper.shared.objects
        let stop = objects.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }
        let trip = objects.trip { trip in
            trip.routePatternId = MapTestDataHelper.shared.patternOrange30.id
        }
        let prediction = objects.prediction { prediction in
            prediction.trip = trip
        }

        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(
            response: .init(objects: objects),
            onGet: { globalLoadSubject.send() }
        )
        repositories.stop = FilteredStopRepository(onGetStopMapData: { stopMapDetailsLoadedPublisher.send() })
        HelpersKt.loadKoinMocks(repositories: repositories)

        let mapVM: MapViewModel = .init(layerManager: MockLayerManager())
        mapVM.allRailSourceData = MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes

        let railRouteShapeRepository = MockRailRouteShapeRepository(response: MapTestDataHelper.shared.routeResponse)

        let nearbyVM: NearbyViewModel = .init(routeCardData: [.init(
            lineOrRoute: .route(MapTestDataHelper.shared.routeOrange),
            stopData: [
                .init(route: MapTestDataHelper.shared.routeOrange, stop: stop, data: [
                    .init(
                        lineOrRoute: .route(MapTestDataHelper.shared.routeOrange),
                        stop: stop,
                        directionId: 0,
                        routePatterns: [MapTestDataHelper.shared.patternOrange30],
                        stopIds: [stop.id],
                        upcomingTrips: [UpcomingTrip(trip: trip, prediction: prediction)],
                        alertsHere: [],
                        allDataLoaded: true,
                        hasSchedulesToday: true,
                        alertsDownstream: [],
                        context: .stopDetailsFiltered
                    ),
                ], globalData: .init(objects: objects)),
            ],
            at: Date.now.toKotlinInstant()
        )])

        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(
            contentVM: .init(),
            mapVM: mapVM,
            nearbyVM: nearbyVM,
            viewportProvider: ViewportProvider(),
            railRouteShapeRepository: railRouteShapeRepository,
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.inspection.inspect(onReceive: globalLoadSubject, after: 1) { sut in
            let newNavStackEntry: SheetNavigationStackEntry =
                .stopDetails(
                    stopId: stop.id,
                    stopFilter: .init(
                        routeId: MapTestDataHelper.shared.routeOrange.id,
                        directionId: MapTestDataHelper.shared.patternOrange30.directionId
                    ),
                    tripFilter: nil
                )
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(relation: .parent, newValue: newNavStackEntry)
        }

        let stopRelatedDataSet = sut.inspection.inspect(onReceive: stopMapDetailsLoadedPublisher, after: 1) { _ in
            XCTAssertFalse(mapVM.routeSourceData.isEmpty)
            XCTAssertTrue(mapVM.routeSourceData.allSatisfy { $0.segmentedShapes.allSatisfy { segment in
                segment.sourceRoutePatternId == MapTestDataHelper.shared.patternOrange30.id
            } })
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, stopRelatedDataSet], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testVehicleTapping() throws {
        class FakeStopRepository: IStopRepository {
            func __getStopMapData(stopId _: String) async throws -> ApiResult<StopMapResponse> {
                ApiResultOk(data: StopMapResponse(
                    routeShapes: MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes,
                    childStops: [:]
                ))
            }
        }
        let repositories = MockRepositories()
        repositories.stop = FakeStopRepository()
        HelpersKt.loadKoinMocks(repositories: repositories)

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }
        let trip = objectCollection.trip { trip in
            trip.routePatternId = MapTestDataHelper.shared.patternOrange30.id
            trip.routeId = MapTestDataHelper.shared.routeOrange.id
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
            vehicle.routeId = MapTestDataHelper.shared.patternOrange30.routeId
            vehicle.directionId = 0
        }

        let nearbyVM: NearbyViewModel = .init(routeCardData: [.init(
            lineOrRoute: .route(MapTestDataHelper.shared.routeOrange),
            stopData: [
                .init(route: MapTestDataHelper.shared.routeOrange, stop: stop, data: [
                    .init(
                        lineOrRoute: .route(MapTestDataHelper.shared.routeOrange),
                        stop: stop,
                        directionId: 0,
                        routePatterns: [MapTestDataHelper.shared.patternOrange30],
                        stopIds: [stop.id],
                        upcomingTrips: [UpcomingTrip(trip: trip, prediction: prediction)],
                        alertsHere: [],
                        allDataLoaded: true,
                        hasSchedulesToday: true,
                        alertsDownstream: [],
                        context: .stopDetailsFiltered
                    ),
                ], globalData: .init(objects: objectCollection)),
            ],
            at: Date.now.toKotlinInstant()
        )])

        let initialNav: SheetNavigationStackEntry = .stopDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: vehicle.routeId!, directionId: vehicle.directionId),
            tripFilter: nil
        )
        nearbyVM.navigationStack = [initialNav]
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(),
            nearbyVM: nearbyVM,
            viewportProvider: ViewportProvider(),
            vehiclesRepository: MockVehiclesRepository(vehicles: [vehicle]),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            XCTAssertEqual(nearbyVM.navigationStack.last, initialNav)
            try sut.find(HomeMapView.self).actualView().handleTapVehicle(vehicle)
            XCTAssertEqual(
                nearbyVM.navigationStack.last,
                .stopDetails(
                    stopId: stop.id,
                    stopFilter: .init(routeId: trip.routeId, directionId: trip.directionId),
                    tripFilter: .init(tripId: trip.id, vehicleId: vehicle.id, stopSequence: 100, selectionLock: true)
                )
            )
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testShowsAllRailShapesWhenSelectedStopCleared() throws {
        let repositories = MockRepositories()
        repositories.stop = FilteredStopRepository(filteredRouteIds: [MapTestDataHelper.shared.routeOrange.id])
        HelpersKt.loadKoinMocks(repositories: repositories)

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }

        let mapVM: MapViewModel = .init(
            allRailSourceData: MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes,
            layerManager: MockLayerManager()
        )

        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: mapVM,
            nearbyVM: .init(navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)]),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(
                relation: .parent,
                newValue: nil as SheetNavigationStackEntry?
            )
            XCTAssertTrue(mapVM.routeSourceData == MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes)
            XCTAssertEqual(mapVM.stopLayerState, .init())
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared], timeout: 5)

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
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let viewportProvider: ViewportProvider = FakeViewportProvider(updateCameraExpectation: updateCameraExpectation)
        let sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(),
            nearbyVM: .init(),
            viewportProvider: viewportProvider,
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        sut.handleCameraChange(.init(
            cameraState: CameraState(
                center: .init(latitude: 2, longitude: 2),
                padding: .zero,
                zoom: 12,
                bearing: .leastNormalMagnitude,
                pitch: 0.0
            ),
            timestamp: Date.now
        ))

        wait(for: [updateCameraExpectation], timeout: 5)
    }

    func testLayersNotReInitWhenAlertsChanges() throws {
        let addLayersNotCalledExpectation = XCTestExpectation(description: "Add layers not called")
        addLayersNotCalledExpectation.isInverted = true
        let updateRouteSourcesExpectation = XCTestExpectation(description: "Update route source called")
        let updateStopSourceExpectation = XCTestExpectation(description: "Update stop source called")

        let layerManager = MockLayerManager(
            addLayersCallback: { addLayersNotCalledExpectation.fulfill() },
            updateRouteDataCallback: { _ in
                updateRouteSourcesExpectation.fulfill()
            },
            updateStopDataCallback: { _ in
                updateStopSourceExpectation.fulfill()
            }
        )
        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(layerManager: layerManager),
            nearbyVM: .init(),
            viewportProvider: ViewportProvider(),
            locationDataManager: .init(),
            sheetHeight: .constant(0),
            globalMapData: GlobalMapData(mapStops: [:], alertsByStop: [:])
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(
                relation: .parent,
                newValue: GlobalMapData(mapStops: [:], alertsByStop: [:])
            )
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared,
                   addLayersNotCalledExpectation,
                   updateRouteSourcesExpectation,
                   updateStopSourceExpectation], timeout: 5)
    }

    func testUpdatesSourcesWhenRouteDataChanges() {
        let updateSourcesCalledExpectation = XCTestExpectation(description: "Update layers called")

        let layerManager = MockLayerManager(updateRouteDataCallback: { _ in
            updateSourcesCalledExpectation.fulfill()
        })

        let routeData = MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes
        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(layerManager: layerManager),
            nearbyVM: .init(),
            viewportProvider: ViewportProvider(),
            locationDataManager: .init(),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(relation: .parent, newValue: routeData)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, updateSourcesCalledExpectation], timeout: 5)
    }

    func testUpdatesStopLayersWhenStateChanges() {
        let addLayersCalledExpectation = XCTestExpectation(description: "Add layers called")

        let layerManager = MockLayerManager(addLayersCallback: {
            addLayersCalledExpectation.fulfill()
        })

        let state: StopLayerGenerator.State = .init(selectedStopId: "stop1")
        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(layerManager: layerManager, globalData: .init(objects: .init())),
            nearbyVM: .init(),
            viewportProvider: ViewportProvider(),
            locationDataManager: .init(),
            sheetHeight: .constant(0),
            globalMapData: .init(mapStops: [:], alertsByStop: [:])
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(relation: .parent, newValue: state)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [hasAppeared, addLayersCalledExpectation], timeout: 5)
    }

    func testJoinsVehiclesChannelOnActiveWhenTripDetails() {
        let joinsVehiclesExp = XCTestExpectation(description: "Joins vehicles channel")

        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(),
            nearbyVM: .init(navigationStack: [
                .stopDetails(
                    stopId: "stop",
                    stopFilter: .init(routeId: "r", directionId: 0),
                    tripFilter: .init(tripId: "t", vehicleId: "v", stopSequence: 0, selectionLock: false)
                ),
            ]),

            viewportProvider: ViewportProvider(),
            vehiclesRepository: CallbackVehiclesRepo(connectExp: joinsVehiclesExp),
            locationDataManager: .init(),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(relation: .parent, newValue: ScenePhase.active)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, joinsVehiclesExp], timeout: 5)
    }

    func testJoinsVehiclesChannelOnActiveWhenFilteredStopDetails() {
        let joinsVehiclesExp = XCTestExpectation(description: "Joins vehicles channel")

        let stop = ObjectCollectionBuilder().stop { _ in }

        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(),
            nearbyVM: .init(navigationStack: [.stopDetails(
                stopId: stop.id,
                stopFilter: .init(routeId: "routeId", directionId: 0),
                tripFilter: nil
            )]),

            viewportProvider: ViewportProvider(),
            vehiclesRepository: CallbackVehiclesRepo(connectExp: joinsVehiclesExp),
            locationDataManager: .init(),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(relation: .parent, newValue: ScenePhase.active)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, joinsVehiclesExp], timeout: 5)
    }

    func testDoesntJoinsVehiclesChannelOnActiveWhenUnfilteredtopDetails() {
        let joinsVehiclesExp = XCTestExpectation(description: "Joins vehicles channel")

        joinsVehiclesExp.isInverted = true

        let stop = ObjectCollectionBuilder().stop { _ in }

        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(),
            nearbyVM: .init(navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)]),
            viewportProvider: ViewportProvider(),
            vehiclesRepository: CallbackVehiclesRepo(connectExp: joinsVehiclesExp),
            locationDataManager: .init(),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(relation: .parent, newValue: ScenePhase.active)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, joinsVehiclesExp], timeout: 5)
    }

    func testLeavesVehiclesChannelOnbackground() {
        let leavesVehiclesExp = XCTestExpectation(description: "Leaves vehicles channel")

        let stop = ObjectCollectionBuilder().stop { _ in }

        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(),
            nearbyVM: .init(navigationStack: [.stopDetails(
                stopId: stop.id,
                stopFilter: .init(routeId: "routeId", directionId: 0),
                tripFilter: nil
            )]),

            viewportProvider: ViewportProvider(),
            vehiclesRepository: CallbackVehiclesRepo(disconnectExp: leavesVehiclesExp),
            locationDataManager: .init(),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).findAndCallOnChange(relation: .parent, newValue: ScenePhase.background)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, leavesVehiclesExp], timeout: 5)
    }

    func testClearsVehiclesOnNavClear() {
        let leavesVehiclesExp = XCTestExpectation(description: "Leaves vehicles channel")

        let stop = ObjectCollectionBuilder().stop { _ in }
        let vehicle = ObjectCollectionBuilder().vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
        }

        var sut = HomeMapView(
            contentVM: .init(),
            mapVM: .init(),
            nearbyVM: .init(navigationStack: [
                .stopDetails(stopId: stop.id, stopFilter: .init(routeId: "routeId", directionId: 0), tripFilter: nil),
            ]),

            viewportProvider: ViewportProvider(),
            vehiclesData: [vehicle],
            vehiclesRepository: CallbackVehiclesRepo(disconnectExp: leavesVehiclesExp),

            locationDataManager: .init(),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { view in
            try view.find(ProxyModifiedMap.self).findAndCallOnChange(
                relation: .parent,
                newValue: nil as SheetNavigationStackEntry?
            )
            XCTAssertEqual(try view.actualView().vehiclesData, [vehicle])
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared], timeout: 5)
    }

    func testRequestsLocationAfterLoading() {
        class FakeLocationFetcher: LocationFetcher {
            var didRequestAuthorization = false

            var locationFetcherDelegate: (any iosApp.LocationFetcherDelegate)?

            var authorizationStatus: CLAuthorizationStatus = .notDetermined

            var accuracyAuthorization: CLAccuracyAuthorization = .fullAccuracy

            var distanceFilter: CLLocationDistance = .zero

            func startUpdatingLocation() {}

            func requestWhenInUseAuthorization() {
                didRequestAuthorization = true
            }
        }
        let contentVM = ContentViewModel()
        let locationFetcher = FakeLocationFetcher()
        var sut = HomeMapView(
            contentVM: contentVM,
            mapVM: .init(),
            nearbyVM: .init(),
            viewportProvider: .init(),
            locationDataManager: .init(locationFetcher: locationFetcher),
            sheetHeight: .constant(0)
        )
        let exp = sut.on(\.didAppear) { view in
            XCTAssertFalse(locationFetcher.didRequestAuthorization)
            contentVM.onboardingScreensPending = []
            try view.findAndCallOnChange(newValue: contentVM.onboardingScreensPending)
            XCTAssertTrue(locationFetcher.didRequestAuthorization)
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    class CallbackVehiclesRepo: IVehiclesRepository {
        var connectExp: XCTestExpectation?
        var disconnectExp: XCTestExpectation?

        init(connectExp: XCTestExpectation? = nil, disconnectExp: XCTestExpectation? = nil) {
            self.connectExp = connectExp
            self.disconnectExp = disconnectExp
        }

        func connect(routeId _: String,
                     directionId _: Int32,
                     onReceive _: @escaping (ApiResult<VehiclesStreamDataResponse>) -> Void) {
            connectExp?.fulfill()
        }

        func disconnect() {
            disconnectExp?.fulfill()
        }
    }
}

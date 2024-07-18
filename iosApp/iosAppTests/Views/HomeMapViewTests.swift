//
//  HomeMapViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
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

    let mockedGlobalResponse: GlobalResponse = .init(
        lines: [:],
        patternIdsByStop: [:],
        routes: [
            MapTestDataHelper.shared.routeOrange.id: MapTestDataHelper.shared.routeOrange,
            MapTestDataHelper.shared.routeRed.id: MapTestDataHelper.shared.routeRed,

        ],
        routePatterns: [:],
        stops: [MapTestDataHelper.shared.stopAssembly.id: MapTestDataHelper.shared.stopAssembly,
                MapTestDataHelper.shared.stopAssemblyChild.id: MapTestDataHelper.shared.stopAssemblyChild,
                MapTestDataHelper.shared.stopSullivan.id: MapTestDataHelper.shared.stopSullivan,
                MapTestDataHelper.shared.stopPorter.id: MapTestDataHelper.shared.stopPorter],
        trips: [:]
    )

    class FilteredStopRepository: IStopRepository {
        private var onGetStopMapData: () -> Void
        private var filteredRouteIds: Set<String>?

        init(filteredRouteIds: Set<String>? = nil, onGetStopMapData: @escaping () -> Void = {}) {
            self.onGetStopMapData = onGetStopMapData
            self.filteredRouteIds = filteredRouteIds
        }

        func __getStopMapData(stopId _: String) async throws -> StopMapResponse {
            onGetStopMapData()

            return StopMapResponse(
                routeShapes: MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes
                    .filter { if let filteredRouteIds = self.filteredRouteIds {
                        filteredRouteIds.contains($0.routeId)
                    } else {
                        true
                    }
                    },
                childStops: [:]
            )
        }
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

    func testUpdatesStopSourceWhenStopSelected() throws {
        HelpersKt.loadKoinMocks(
            repositories: MockRepositories.companion.buildWithDefaults(
                stop: FilteredStopRepository(filteredRouteIds: [MapTestDataHelper.shared.routeOrange.id])
            )
        )

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }

        let mapVM: MapViewModel = .init(layerManager: MockLayerManager())

        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.shared.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: mapVM,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            XCTAssertEqual(mapVM.stopSourceData, .init())
            let newNavStackEntry: SheetNavigationStackEntry = .stopDetails(stop, nil)
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: newNavStackEntry)
            XCTAssertEqual(mapVM.stopSourceData, .init(selectedStopId: stop.id))
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testUpdatesRouteAndChildStopsWhenStopSelected() throws {
        let globalLoadSubject = PassthroughSubject<Void, Never>()

        let stopMapDetailsLoadedPublisher = PassthroughSubject<Void, Never>()
        HelpersKt
            .loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(stop:
                FilteredStopRepository(filteredRouteIds: [MapTestDataHelper.shared.routeOrange.id],
                                       onGetStopMapData: { stopMapDetailsLoadedPublisher.send() }),
                global: MockGlobalRepository(
                    response: mockedGlobalResponse,
                    onGet: { globalLoadSubject.send(
                    ) }
                )))

        let mapVM: MapViewModel = .init(layerManager: MockLayerManager())
        mapVM.allRailSourceData = MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }

        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.shared.routeResponse
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: mapVM,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.inspection.inspect(onReceive: globalLoadSubject, after: 0.2) { sut in
            let newNavStackEntry: SheetNavigationStackEntry = .stopDetails(stop, nil)
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: newNavStackEntry)
        }

        ViewHosting.host(view: sut)

        let stopRelatedDataSet = sut.inspection.inspect(onReceive: stopMapDetailsLoadedPublisher, after: 0.2) { _ in
            XCTAssertFalse(mapVM.routeSourceData.isEmpty)
            XCTAssertTrue(mapVM.routeSourceData.allSatisfy { $0.routeId == MapTestDataHelper.shared.routeOrange.id })
            XCTAssertNotNil(mapVM.childStops)
        }
        wait(for: [hasAppeared, stopRelatedDataSet], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testSetsRouteSourceWhenStopSelectedWithRouteFilter() throws {
        let globalLoadSubject = PassthroughSubject<Void, Never>()
        let stopMapDetailsLoadedPublisher = PassthroughSubject<Void, Never>()
        HelpersKt
            .loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(stop:
                FilteredStopRepository(
                    filteredRouteIds: [MapTestDataHelper.shared.routeOrange.id],
                    onGetStopMapData: { stopMapDetailsLoadedPublisher.send() }
                ),
                global: MockGlobalRepository(
                    response: mockedGlobalResponse,
                    onGet: { globalLoadSubject.send(
                    ) }
                )))

        let mapVM: MapViewModel = .init(layerManager: MockLayerManager())
        mapVM.allRailSourceData = MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }

        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.shared.routeResponse

        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: mapVM,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.inspection.inspect(onReceive: globalLoadSubject, after: 0.2) { sut in
            let newNavStackEntry: SheetNavigationStackEntry =
                .stopDetails(stop, .init(routeId: MapTestDataHelper.shared.routeOrange.id,
                                         directionId: MapTestDataHelper.shared.patternOrange30.directionId))
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: newNavStackEntry)
        }

        let stopRelatedDataSet = sut.inspection.inspect(onReceive: stopMapDetailsLoadedPublisher, after: 0.2) { _ in
            XCTAssertFalse(mapVM.routeSourceData.isEmpty)
            XCTAssertTrue(mapVM.routeSourceData.allSatisfy { $0.routeId == MapTestDataHelper.shared.routeOrange.id })
            XCTAssertNotNil(mapVM.childStops)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, stopRelatedDataSet], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testUpdatesRouteSourceWhenStopSelectedWithRouteFilterAndUpcomingDepartures() throws {
        let globalLoadSubject = PassthroughSubject<Void, Never>()
        let stopMapDetailsLoadedPublisher = PassthroughSubject<Void, Never>()
        HelpersKt
            .loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(stop:
                FilteredStopRepository(onGetStopMapData: { stopMapDetailsLoadedPublisher.send() }),
                global: MockGlobalRepository(
                    response: mockedGlobalResponse,
                    onGet: { globalLoadSubject.send(
                    ) }

                )))

        let mapVM: MapViewModel = .init(layerManager: MockLayerManager())
        mapVM.allRailSourceData = MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes

        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.shared.routeResponse

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }
        let trip = objectCollection.trip { trip in
            trip.routePatternId = MapTestDataHelper.shared.patternOrange30.id
        }

        let prediction = objectCollection.prediction { prediction in
            prediction.trip = trip
        }

        let nearbyVM: NearbyViewModel = .init()
        nearbyVM.setDepartures(StopDetailsDepartures(routes:
            [.init(route: MapTestDataHelper.shared.routeOrange, stop: stop,
                   patterns: [.ByHeadsign(
                       route: MapTestDataHelper.shared.routeOrange,
                       headsign: MapTestDataHelper.shared.tripOrangeC1.headsign,
                       line: nil,
                       patterns: [MapTestDataHelper.shared.patternOrange30],
                       upcomingTrips: [UpcomingTrip(trip: trip, prediction: prediction)],
                       alertsHere: nil
                   )])]))

        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: mapVM,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.inspection.inspect(onReceive: globalLoadSubject, after: 0.2) { sut in
            let newNavStackEntry: SheetNavigationStackEntry =
                .stopDetails(stop, .init(routeId: MapTestDataHelper.shared.routeOrange.id,
                                         directionId: MapTestDataHelper.shared.patternOrange30.directionId))
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: newNavStackEntry)
        }

        let stopRelatedDataSet = sut.inspection.inspect(onReceive: stopMapDetailsLoadedPublisher, after: 0.2) { _ in
            XCTAssertFalse(mapVM.routeSourceData.isEmpty)
            XCTAssertTrue(mapVM.routeSourceData.allSatisfy { $0.segmentedShapes.allSatisfy { segment in
                segment.sourceRoutePatternId == MapTestDataHelper.shared.patternOrange30.id
            } })
            XCTAssertNotNil(mapVM.childStops)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, stopRelatedDataSet], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testUpdatesSourcesWhenTripSelected() throws {
        let globalLoadSubject = PassthroughSubject<Void, Never>()
        let tripShapeLoadSubject = PassthroughSubject<Void, Never>()

        class FakeTripRepository: IdleTripRepository {
            private var onGetTripShape: () -> Void
            init(onGetTripShape: @escaping () -> Void) {
                self.onGetTripShape = onGetTripShape
            }

            override func __getTripShape(tripId _: String) async throws -> ApiResult<TripShape> {
                onGetTripShape()
                return ApiResultOk(
                    data: .init(
                        shapeWithStops: .init(
                            directionId: 1,
                            routeId: MapTestDataHelper.shared.routeOrange.id,
                            routePatternId: MapTestDataHelper.shared.patternOrange30.id,
                            shape: MapTestDataHelper.shared.shapeOrangeC1,
                            stopIds: [
                                MapTestDataHelper.shared.stopAssemblyChild.id,
                                MapTestDataHelper.shared.stopSullivan.id,
                            ]
                        )
                    )
                )
            }
        }

        HelpersKt
            .loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(
                trip: FakeTripRepository(onGetTripShape: { tripShapeLoadSubject.send() }),
                global: MockGlobalRepository(response: mockedGlobalResponse, onGet: { globalLoadSubject.send() })
            ))

        let mapVM: MapViewModel = .init(layerManager: MockLayerManager())
        mapVM.allRailSourceData = MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes

        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        railRouteShapeFetcher.response = MapTestDataHelper.shared.routeResponse

        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: mapVM,
            nearbyVM: .init(),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let globalDataLoaded = sut.inspection.inspect(onReceive: globalLoadSubject, after: 0.2) { sut in
            let newNavStackEntry: SheetNavigationStackEntry =
                .tripDetails(tripId: "ol_trip_id",
                             vehicleId: "vehicle",
                             target: .init(stopId: MapTestDataHelper.shared.stopSullivan.id,
                                           stopSequence: 0),
                             routeId: MapTestDataHelper.shared.routeOrange.id,
                             directionId: MapTestDataHelper.shared
                                 .patternOrange30
                                 .directionId)
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: newNavStackEntry)
        }

        let routeDataSet = sut.inspection.inspect(onReceive: tripShapeLoadSubject, after: 0.2) { _ in
            XCTAssertFalse(mapVM.routeSourceData.isEmpty)
            XCTAssertTrue(mapVM.routeSourceData.allSatisfy { $0.segmentedShapes.allSatisfy { segment in
                segment.sourceRoutePatternId == MapTestDataHelper.shared.patternOrange30.id
            }})
            XCTAssertNil(mapVM.childStops)
        }

        let stopDataSet = sut.inspection.inspect(onReceive: tripShapeLoadSubject, after: 0.2) { _ in
            XCTAssertEqual(mapVM.stopSourceData, .init(filteredStopIds: [MapTestDataHelper.shared.stopAssembly.id,
                                                                         MapTestDataHelper.shared.stopSullivan.id],
                                                       selectedStopId: MapTestDataHelper.shared.stopSullivan.id))
        }

        ViewHosting.host(view: sut)
        wait(for: [globalDataLoaded, routeDataSet, stopDataSet], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testVehicleTapping() throws {
        class FakeStopRepository: IStopRepository {
            func __getStopMapData(stopId _: String) async throws -> StopMapResponse {
                StopMapResponse(
                    routeShapes: MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes,
                    childStops: [:]
                )
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

        let nearbyVM: NearbyViewModel = .init()
        nearbyVM.setDepartures(StopDetailsDepartures(routes:
            [.init(route: MapTestDataHelper.shared.routeOrange, stop: stop,
                   patterns: [.ByHeadsign(route: MapTestDataHelper.shared.routeOrange,
                                          headsign: MapTestDataHelper.shared.tripOrangeC1.headsign,
                                          line: nil,
                                          patterns: [MapTestDataHelper.shared.patternOrange30],
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

    func testVehicleChanging() throws {
        class FakeStopRepository: IStopRepository {
            func __getStopMapData(stopId _: String) async throws -> StopMapResponse {
                StopMapResponse(routeShapes: MapTestDataHelper.routeResponse.routesWithSegmentedShapes, childStops: [:])
            }
        }
        HelpersKt.loadKoinMocks(repositories: MockRepositories.companion.buildWithDefaults(stop: FakeStopRepository()))

        let objectCollection = ObjectCollectionBuilder()
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = -1
            stop.longitude = -1
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

        let vehicle1 = objectCollection.vehicle { vehicle in
            vehicle.id = "1"
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = trip.id
            vehicle.routeId = MapTestDataHelper.patternOrange30.routeId
            vehicle.directionId = 0
            vehicle.latitude = 0
            vehicle.longitude = 0
        }
        let vehicle2 = objectCollection.vehicle { vehicle in
            vehicle.id = "1"
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = trip.id
            vehicle.routeId = MapTestDataHelper.patternOrange30.routeId
            vehicle.directionId = 0
            vehicle.latitude = 1
            vehicle.longitude = 1
        }
        let vehicle3 = objectCollection.vehicle { vehicle in
            vehicle.id = "2"
            vehicle.currentStatus = .inTransitTo
            vehicle.tripId = trip.id
            vehicle.routeId = MapTestDataHelper.patternOrange30.routeId
            vehicle.directionId = 0
            vehicle.latitude = 2
            vehicle.longitude = 2
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
            .init(routeId: vehicle1.routeId!, directionId: vehicle1.directionId)
        )
        let mapVM: MapViewModel = .init(allRailSourceData: MapTestDataHelper.routeResponse.routesWithSegmentedShapes,
                                        layerManager: MockLayerManager())
        nearbyVM.navigationStack = [initialNav]
        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())

        let viewportProvider = ViewportProvider()
        var sut = HomeMapView(
            mapVM: mapVM,
            nearbyVM: nearbyVM,
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket(), vehicles: [vehicle1]),
            viewportProvider: viewportProvider,
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.actualView().globalData = GlobalResponse(objects: objectCollection, patternIdsByStop: [:])
            XCTAssertEqual(nearbyVM.navigationStack.last, initialNav)
            XCTAssertFalse(viewportProvider.isFollowingVehicle)
            nearbyVM.navigationStack.append(.tripDetails(
                tripId: trip.id,
                vehicleId: vehicle1.id,
                target: .some(.init(stopId: stop.id, stopSequence: 0)),
                routeId: "",
                directionId: 0
            ))
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: vehicle1)
        }

        let following1 = sut.inspection.inspect(
            onReceive: viewportProvider.$followedVehicle, after: 1.1
        ) { sut in
            XCTAssertTrue(viewportProvider.isFollowingVehicle)
            XCTAssertEqual(viewportProvider.followedVehicle, vehicle1)
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: vehicle2)
        }

        let following2 = sut.inspection.inspect(
            onReceive: viewportProvider.$followedVehicle.dropFirst(), after: 1.1
        ) { sut in
            // Viewport setting happens after the followed vehicle is set,
            // so this is actually the viewport for vehicle 1
            XCTAssertNotNil(viewportProvider.viewport.overview)
            XCTAssertTrue(viewportProvider.isFollowingVehicle)
            XCTAssertEqual(viewportProvider.followedVehicle, vehicle2)
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: vehicle3)
        }

        let following3 = sut.inspection.inspect(
            onReceive: viewportProvider.$followedVehicle.dropFirst(2), after: 1.1
        ) { sut in
            XCTAssertTrue(viewportProvider.isFollowingVehicle)
            XCTAssertEqual(viewportProvider.followedVehicle, vehicle3)
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: nil as Vehicle?)
        }

        let following4 = sut.inspection.inspect(
            onReceive: viewportProvider.$followedVehicle.dropFirst(3), after: 1.1
        ) { _ in
            // And this is checking the viewport for vehicle 3
            XCTAssertNotNil(viewportProvider.viewport.overview)
            XCTAssertFalse(viewportProvider.isFollowingVehicle)
            XCTAssertEqual(viewportProvider.followedVehicle, nil)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, following1, following2, following3, following4], timeout: 15)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testShowsAllRailShapesWhenSelectedStopCleared() throws {
        HelpersKt.loadKoinMocks(
            repositories: MockRepositories.companion.buildWithDefaults(
                stop: FilteredStopRepository(filteredRouteIds: [MapTestDataHelper.shared.routeOrange.id])
            )
        )

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

        let railRouteShapeFetcher: RailRouteShapeFetcher = .init(backend: IdleBackend())
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        var sut = HomeMapView(
            mapVM: mapVM,
            nearbyVM: .init(navigationStack: [.stopDetails(stop, nil)]),
            railRouteShapeFetcher: railRouteShapeFetcher,
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: locationDataManager,
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: nil as SheetNavigationStackEntry?)
            XCTAssertTrue(mapVM.routeSourceData == MapTestDataHelper.shared.routeResponse.routesWithSegmentedShapes)
            XCTAssertEqual(mapVM.stopSourceData, .init())
        }

        ViewHosting.host(view: sut)
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
            sheetHeight: .constant(0)
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

        let layerManger: IMapLayerManager = MockLayerManager(
            addLayersCallback: { addLayersCalledExpectation.fulfill() },
            updateRouteDataCallback: { _ in updateSourcesCalledExpectation.fulfill() }
        )

        var sut = HomeMapView(
            mapVM: .init(layerManager: layerManger),
            nearbyVM: .init(),
            railRouteShapeFetcher: .init(backend: IdleBackend()),
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: .init(),
            sheetHeight: .constant(0)
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
        let updateRouteSourcesExpectation = XCTestExpectation(description: "Update route source called")
        let updateStopSourceExpectation = XCTestExpectation(description: "Update stop source called")

        let layerManager = MockLayerManager(addLayersCallback: { addLayersNotCalledExpectation.fulfill() },
                                            updateRouteDataCallback: { _ in
                                                updateRouteSourcesExpectation.fulfill()
                                            },
                                            updateStopSourceCallback: { _ in
                                                updateStopSourceExpectation.fulfill()
                                            })
        var sut = HomeMapView(
            mapVM: .init(layerManager: layerManager),
            nearbyVM: .init(),
            railRouteShapeFetcher: .init(backend: IdleBackend()),
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: .init(),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: GlobalMapData(mapStops: [:], alertsByStop: [:]))
        }

        ViewHosting.host(view: sut)
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
            mapVM: .init(layerManager: layerManager),
            nearbyVM: .init(),
            railRouteShapeFetcher: .init(backend: IdleBackend()),
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: .init(),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: routeData)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, updateSourcesCalledExpectation], timeout: 5)
    }

    func testUpdatesStopSourcesWhenStopDataChanges() {
        let updateStopSourcesCalledExpectation = XCTestExpectation(description: "Update stop source called")

        let layerManager = MockLayerManager(updateStopSourceCallback: { _ in
            updateStopSourcesCalledExpectation.fulfill()
        })

        let stopData: StopSourceData = .init(selectedStopId: "stop1")
        var sut = HomeMapView(
            mapVM: .init(layerManager: layerManager),
            nearbyVM: .init(),
            railRouteShapeFetcher: .init(backend: IdleBackend()),
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: .init(),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: stopData)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, updateStopSourcesCalledExpectation], timeout: 5)
    }

    func testUpdatesChildStopSourcesWhenDataChanges() {
        let updateChildStopSourcesCalledExpectation = XCTestExpectation(description: "Update child stop source called")

        let layerManager = MockLayerManager(updateChildStopSourceCallback: { _ in
            updateChildStopSourcesCalledExpectation.fulfill()
        })

        let childStopData: [String: Stop]? = ["stop1": ObjectCollectionBuilder().stop { _ in }]
        var sut = HomeMapView(
            mapVM: .init(layerManager: layerManager),
            nearbyVM: .init(),
            railRouteShapeFetcher: .init(backend: IdleBackend()),
            vehiclesFetcher: .init(socket: MockSocket()),
            viewportProvider: ViewportProvider(),
            locationDataManager: .init(),
            sheetHeight: .constant(0)
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(ProxyModifiedMap.self).callOnChange(newValue: childStopData)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, updateChildStopSourcesCalledExpectation], timeout: 5)
    }
}

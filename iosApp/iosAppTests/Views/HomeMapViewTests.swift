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
        private var filteredRouteIds: Set<LineOrRoute.Id>?

        init(filteredRouteIds: Set<LineOrRoute.Id>? = nil, onGetStopMapData: @escaping () -> Void = {}) {
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

    @MainActor func testAppears() {
        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let sheetHeight: Binding<CGFloat> = .constant(100)

        let sut = HomeMapView(
            alerts: .init(alerts: [:]),
            contentVM: .init(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            sheetHeight: sheetHeight,
            selectedVehicle: .constant(nil),
            locationDataManager: .init(),
            navManager: .init(),
            viewportProvider: viewportProvider,
        )

        let exp = sut.inspection.inspect { view in
            XCTAssertNotNil(view)
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    func testNoMapWhenLocationAuthNotKnown() {
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(
            alerts: .init(alerts: [:]),
            contentVM: .init(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            sheetHeight: .constant(0),
            selectedVehicle: .constant(nil),
            locationDataManager: locationDataManager,
            navManager: .init(),
            viewportProvider: ViewportProvider(),
        )

        XCTAssertNotNil(try sut.withFixedSettings([:]).inspect().find(viewWithAccessibilityIdentifier: "emptyMapGrid"))
    }

    @MainActor
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
        let route = MapTestDataHelper.shared.routeOrange
        objectCollection.routes[route.id] = route
        let pattern = MapTestDataHelper.shared.patternOrange30
        objectCollection.routePatterns[pattern.id] = pattern
        let stop = objectCollection.stop { stop in
            stop.id = "1"
            stop.latitude = 1
            stop.longitude = 1
        }
        let trip = objectCollection.trip { trip in
            trip.routePatternId = pattern.id
            trip.routeId = route.id.idText
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
            vehicle.routeId = route.id.idText
            vehicle.directionId = 0
        }

        let routeCardData: [RouteCardData] = [.init(
            lineOrRoute: .route(route),
            stopData: [
                .init(route: route, stop: stop, data: [
                    .init(
                        lineOrRoute: .route(route),
                        stop: stop,
                        direction: .init(directionId: 0, route: route),
                        routePatterns: [pattern],
                        stopIds: [stop.id],
                        upcomingTrips: [UpcomingTrip(trip: trip, prediction: prediction, vehicle: vehicle)],
                        alertsHere: [],
                        allDataLoaded: true,
                        hasSchedulesToday: true,
                        subwayServiceStartTime: nil,
                        alertsDownstream: [],
                        context: .stopDetailsFiltered
                    ),
                ]),
            ],
            at: EasternTimeInstant.now()
        )]
        let routeCardDataVM = MockRouteCardDataViewModel(initialState: .init(data: routeCardData))
        let nearbyVM = MockNearbyViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            routeCardData: routeCardData,
            loadedLocation: stop.position,
            loadedStopIds: [stop.id],
        ))

        let initialNav: SheetNavigationStackEntry = try .stopDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: XCTUnwrap(vehicle.routeId), directionId: vehicle.directionId),
            tripFilter: nil
        )
        let navManager = NavigationManager(navigationStack: [initialNav])
        let locationDataManager: LocationDataManager = .init(locationFetcher: MockLocationFetcher())
        let sut = HomeMapView(
            alerts: .init(alerts: [:]),
            contentVM: .init(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: routeCardDataVM,
            vehiclesRepository: MockVehiclesRepository(vehicles: [vehicle]),
            sheetHeight: .constant(0),
            selectedVehicle: .constant(nil),
            locationDataManager: locationDataManager,
            navManager: navManager,
            viewportProvider: ViewportProvider(),
        )

        let hasAppeared = sut.inspection.inspect(after: 1) { sut in
            XCTAssertEqual(navManager.navigationStack.last, initialNav)
            try sut.find(HomeMapView.self).actualView().handleTapVehicle(vehicle)
            XCTAssertEqual(
                navManager.navigationStack.last,
                .tripDetails(filter: .init(
                    tripId: trip.id,
                    vehicleId: vehicle.id,
                    routeId: trip.routeId,
                    directionId: trip.directionId,
                    stopId: stop.id,
                    stopSequence: 100
                ))
            )
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared], timeout: 5)

        addTeardownBlock {
            HelpersKt.loadDefaultRepoModules()
        }
    }

    func testUpdatesViewportOnCameraChangeBeforeLayersLoad() {
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
        let viewportProvider: ViewportProvider = FakeViewportProvider(updateCameraExpectation:
            updateCameraExpectation)
        let sut = HomeMapView(
            alerts: .init(alerts: [:]),
            contentVM: .init(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            sheetHeight: .constant(0),
            selectedVehicle: .constant(nil),
            locationDataManager: locationDataManager,
            navManager: .init(),
            viewportProvider: viewportProvider,
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

    func testJoinsVehiclesChannelOnActiveWhenTripDetails() {
        let joinsVehiclesExp = XCTestExpectation(description: "Joins vehicles channel")
        let navManager = NavigationManager(navigationStack: [
            .stopDetails(
                stopId: "stop",
                stopFilter: .init(routeId: Route.Id("r"), directionId: 0),
                tripFilter: .init(tripId: "t", vehicleId: "v", stopSequence: 0, selectionLock: false)
            ),
        ])

        var sut = HomeMapView(
            alerts: .init(alerts: [:]),
            contentVM: .init(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            vehiclesRepository: CallbackVehiclesRepo(connectExp: joinsVehiclesExp),
            sheetHeight: .constant(0),
            selectedVehicle: .constant(nil),
            locationDataManager: .init(),
            navManager: navManager,
            viewportProvider: ViewportProvider(viewport: .camera(center: ViewportProvider.Defaults.center)),
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(AnnotatedMap.self).findAndCallOnChange(relation: .parent, newValue: ScenePhase.active)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, joinsVehiclesExp], timeout: 5)
    }

    func testJoinsVehiclesChannelOnActiveWhenFilteredStopDetails() {
        let joinsVehiclesExp = XCTestExpectation(description: "Joins vehicles channel")

        let stop = ObjectCollectionBuilder().stop { _ in }
        let navManager = NavigationManager(navigationStack: [.stopDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: Route.Id("routeId"), directionId: 0),
            tripFilter: nil
        )])

        var sut = HomeMapView(
            alerts: .init(alerts: [:]),
            contentVM: .init(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            vehiclesRepository: CallbackVehiclesRepo(connectExp: joinsVehiclesExp),
            sheetHeight: .constant(0),
            selectedVehicle: .constant(nil),
            locationDataManager: .init(),
            navManager: navManager,
            viewportProvider: ViewportProvider(viewport: .camera(center: ViewportProvider.Defaults.center)),
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(AnnotatedMap.self).findAndCallOnChange(relation: .parent, newValue: ScenePhase.active)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, joinsVehiclesExp], timeout: 5)
    }

    func testDoesntJoinsVehiclesChannelOnActiveWhenUnfilteredtopDetails() {
        let joinsVehiclesExp = XCTestExpectation(description: "Joins vehicles channel")

        joinsVehiclesExp.isInverted = true

        let stop = ObjectCollectionBuilder().stop { _ in }
        let navManager = NavigationManager(navigationStack: [.stopDetails(
            stopId: stop.id,
            stopFilter: nil,
            tripFilter: nil
        )])

        var sut = HomeMapView(
            alerts: .init(alerts: [:]),
            contentVM: .init(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            vehiclesRepository: CallbackVehiclesRepo(connectExp: joinsVehiclesExp),
            sheetHeight: .constant(0),
            selectedVehicle: .constant(nil),
            locationDataManager: .init(),
            navManager: navManager,
            viewportProvider: ViewportProvider(viewport: .camera(center: ViewportProvider.Defaults.center)),
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(AnnotatedMap.self).findAndCallOnChange(relation: .parent, newValue: ScenePhase.active)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, joinsVehiclesExp], timeout: 5)
    }

    func testLeavesVehiclesChannelOnbackground() {
        let leavesVehiclesExp = XCTestExpectation(description: "Leaves vehicles channel")

        let stop = ObjectCollectionBuilder().stop { _ in }
        let navManager = NavigationManager(navigationStack: [.stopDetails(
            stopId: stop.id,
            stopFilter: .init(routeId: Route.Id("routeId"), directionId: 0),
            tripFilter: nil
        )])

        var sut = HomeMapView(
            alerts: .init(alerts: [:]),
            contentVM: .init(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            vehiclesRepository: CallbackVehiclesRepo(disconnectExp: leavesVehiclesExp),
            sheetHeight: .constant(0),
            selectedVehicle: .constant(nil),
            locationDataManager: .init(),
            navManager: navManager,
            viewportProvider: ViewportProvider(viewport: .camera(center: ViewportProvider.Defaults.center)),
        )

        let hasAppeared = sut.on(\.didAppear) { sut in
            try sut.find(AnnotatedMap.self).findAndCallOnChange(relation: .parent, newValue:
                ScenePhase.background)
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
        let navManager = NavigationManager(navigationStack: [
            .stopDetails(
                stopId: stop.id,
                stopFilter: .init(routeId: Route.Id("routeId"), directionId: 0),
                tripFilter: nil
            ),
        ])

        var sut = HomeMapView(
            alerts: .init(alerts: [:]),
            contentVM: .init(),
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            vehiclesData: [vehicle],
            vehiclesRepository: CallbackVehiclesRepo(disconnectExp: leavesVehiclesExp),
            sheetHeight: .constant(0),
            selectedVehicle: .constant(nil),
            locationDataManager: .init(),
            navManager: navManager,
            viewportProvider: ViewportProvider(viewport: .camera(center: ViewportProvider.Defaults.center)),
        )

        let hasAppeared = sut.on(\.didAppear) { view in
            try view.find(AnnotatedMap.self).findAndCallOnChange(
                relation: .parent,
                newValue: [SheetNavigationStackEntry]()
            )
            XCTAssertEqual(try view.actualView().vehiclesData, [vehicle])
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared], timeout: 5)
    }

    @MainActor
    func testRequestsLocationAfterLoading() {
        class FakeLocationFetcher: LocationFetcher {
            var didRequestAuthorization = false

            var locationFetcherDelegate: (any LocationFetcherDelegate)?

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
            alerts: .init(alerts: [:]),
            contentVM: contentVM,
            mapVM: MockMapViewModel(),
            routeCardDataVM: MockRouteCardDataViewModel(),
            sheetHeight: .constant(0),
            selectedVehicle: .constant(nil),
            locationDataManager: .init(locationFetcher: locationFetcher),
            navManager: .init(),
            viewportProvider: .init(),
        )
        let exp = sut.inspection.inspect(after: 1) { view in
            XCTAssertFalse(locationFetcher.didRequestAuthorization)
            contentVM.onboardingScreensPending = []
            try view.findAndCallOnChange(newValue: contentVM.onboardingScreensPending)
            XCTAssertTrue(locationFetcher.didRequestAuthorization)
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    class CallbackVehiclesRepo: IVehiclesRepository {
        var connectExp: XCTestExpectation?
        var disconnectExp: XCTestExpectation?

        init(connectExp: XCTestExpectation? = nil, disconnectExp: XCTestExpectation? = nil) {
            self.connectExp = connectExp
            self.disconnectExp = disconnectExp
        }

        func connect(routeId _: LineOrRoute.Id,
                     directionId _: Int32,
                     errorKey _: ErrorKey,
                     onReceive _: @escaping (ApiResult<VehiclesStreamDataResponse>) -> Void) {
            connectExp?.fulfill()
        }

        func disconnect() {
            disconnectExp?.fulfill()
        }
    }
}

//
//  TripDetailsPageTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-05-08.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import shared
import ViewInspector
import XCTest

final class TripDetailsPageTests: XCTestCase {
    @MainActor func testLoadsStopList() throws {
        let objects = ObjectCollectionBuilder()

        let trip = objects.trip { _ in }

        let stop1 = objects.stop { stop in
            stop.name = "Somewhere"
        }
        let stop2 = objects.stop { stop in
            stop.name = "Elsewhere"
        }

        objects.prediction { prediction in
            prediction.stopId = stop1.id
            prediction.stopSequence = 1
            prediction.departureTime = Date.now.addingTimeInterval(15).toKotlinInstant()
        }

        objects.prediction { prediction in
            prediction.stopId = stop2.id
            prediction.stopSequence = 2
            prediction.departureTime = Date.now.addingTimeInterval(30).toKotlinInstant()
        }

        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
        }

        let tripSchedulesLoaded = PassthroughSubject<Void, Never>()

        let tripRepository = FakeTripRepository(
            tripResponse: TripResponse(trip: trip),
            scheduleResponse: TripSchedulesResponse.StopIds(stopIds: [stop1.id, stop2.id]),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let tripPredictionsRepository = FakeTripPredictionsRepository(response: .init(objects: objects))

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        let tripId = trip.id
        let vehicleId = "999"
        var sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            routeId: trip.routeId,
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            globalRepository: MockGlobalRepository(response: .init(objects: objects, patternIdsByStop: [:])),
            tripPredictionsRepository: tripPredictionsRepository,
            tripRepository: tripRepository,
            vehicleRepository: MockVehicleRepository(outcome: ApiResultOk(data: .init(vehicle: vehicle)))
        )

        let showsStopsExp = sut.on(\.didLoadData) { view in
            XCTAssertNotNil(try view.find(text: "Somewhere"))
            XCTAssertNotNil(try view.find(text: "Elsewhere"))
        }

        ViewHosting.host(view: sut)

        wait(for: [showsStopsExp], timeout: 5)
    }

    @MainActor func testIncludesVehicleCard() throws {
        let objects = ObjectCollectionBuilder()

        let stop1 = objects.stop { stop in
            stop.name = "Somewhere"
        }

        let route = objects.route { route in
            route.id = "Red"
        }

        let trip = objects.trip { trip in
            trip.routeId = route.id
        }

        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.stopId = stop1.id
            vehicle.currentStatus = .inTransitTo
        }

        objects.prediction { prediction in
            prediction.stopId = stop1.id
            prediction.tripId = trip.id
            prediction.vehicleId = vehicle.id
            prediction.stopSequence = 1
            prediction.departureTime = Date.now.toKotlinInstant()
        }

        let tripSchedulesLoaded = PassthroughSubject<Void, Never>()

        let tripRepository = FakeTripRepository(
            tripResponse: .init(trip: trip),
            scheduleResponse: TripSchedulesResponse.StopIds(stopIds: [stop1.id]),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let tripPredictionsRepository = FakeTripPredictionsRepository(response: .init(objects: objects))

        let tripId = trip.id
        let vehicleId = vehicle.id
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        var sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            routeId: route.id,
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            globalRepository: MockGlobalRepository(response: .init(objects: objects, patternIdsByStop: [:])),
            tripPredictionsRepository: tripPredictionsRepository,
            tripRepository: tripRepository,
            vehicleRepository: FakeVehicleRepository(response: .init(vehicle: vehicle))
        )

        let showVehicleCardExp = sut.on(\.didLoadData) { view in
            XCTAssertNotNil(try view.find(VehicleOnTripView.self))
        }

        ViewHosting.host(view: sut)

        wait(for: [showVehicleCardExp], timeout: 5)
    }

    @MainActor func testSplitsWithTarget() throws {
        let objects = ObjectCollectionBuilder()

        let stop1 = objects.stop { stop in
            stop.name = "Somewhere"
        }
        let stop2 = objects.stop { stop in
            stop.name = "Elsewhere"
        }
        let stop3 = objects.stop { _ in }
        let stop4 = objects.stop { _ in }

        let trip = objects.trip { _ in }

        let tripSchedulesLoaded = PassthroughSubject<Void, Never>()

        let tripRepository = FakeTripRepository(
            tripResponse: .init(trip: trip),
            scheduleResponse: TripSchedulesResponse.StopIds(stopIds: [stop1.id, stop2.id, stop3.id, stop4.id]),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .inTransitTo
        }
        let tripId = trip.id
        let vehicleId = "999"
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        let sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            routeId: trip.routeId,
            target: .init(stopId: stop3.id, stopSequence: 998),
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            tripPredictionsRepository: FakeTripPredictionsRepository(response: .init(objects: objects)),
            tripRepository: tripRepository,
            vehicleRepository: FakeVehicleRepository(response: .init(vehicle: vehicle))
        )

        let splitViewExp = sut.inspection.inspect(onReceive: tripSchedulesLoaded, after: 0.5) { view in
            XCTAssertNotNil(try view.find(TripDetailsStopListSplitView.self))
        }

        ViewHosting.host(view: sut)

        wait(for: [splitViewExp], timeout: 5)
    }

    @MainActor func testDisplaysTransferRoutes() throws {
        let objects = ObjectCollectionBuilder()

        let stop1 = objects.stop { stop in
            stop.name = "Somewhere"
        }
        let stop2 = objects.stop { stop in
            stop.name = "Elsewhere"
        }

        let route1 = objects.route {
            $0.id = "Red"
            $0.type = .heavyRail
            $0.longName = "Red Line"
        }
        let route2 = objects.route {
            $0.id = "Green"
            $0.type = .lightRail
            $0.longName = "Green Line"
        }
        let route3 = objects.route {
            $0.id = "1"
            $0.type = .bus
            $0.shortName = "1"
        }

        let pattern1 = objects.routePattern(route: route1) {
            $0.id = "Red-1"
            $0.typicality = .typical
        }
        let pattern2 = objects.routePattern(route: route2) {
            $0.id = "Green-1"
            $0.typicality = .typical
        }
        let pattern3 = objects.routePattern(route: route3) {
            $0.id = "1-1"
            $0.typicality = .typical
        }

        let trip = objects.trip { trip in
            trip.routeId = "Red"
        }

        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.stopId = stop1.id
            vehicle.currentStatus = .inTransitTo
        }

        objects.prediction { prediction in
            prediction.stopId = stop1.id
            prediction.tripId = trip.id
            prediction.vehicleId = vehicle.id
            prediction.routeId = route1.id
            prediction.stopSequence = 1
            prediction.departureTime = Date.now.toKotlinInstant()
        }
        objects.prediction { prediction in
            prediction.stopId = stop2.id
            prediction.tripId = trip.id
            prediction.vehicleId = vehicle.id
            prediction.routeId = route1.id
            prediction.stopSequence = 2
            prediction.departureTime = Date.now.toKotlinInstant().plus(duration: 100)
        }

        let globalData = GlobalResponse(objects: objects, patternIdsByStop: [
            stop1.id: [pattern1.id, pattern2.id],
            stop2.id: [pattern1.id, pattern3.id],
        ])

        let tripRepository = FakeTripRepository(
            tripResponse: .init(trip: trip),
            scheduleResponse: TripSchedulesResponse.StopIds(stopIds: [stop1.id, stop2.id])
        )

        let tripPredictionsRepository = FakeTripPredictionsRepository(
            response: .init(objects: objects)
        )

        let nearbyVM = NearbyViewModel(alertsRepository: MockAlertsRepository())
        nearbyVM.alerts = .init(alerts: [:])
        let tripId = trip.id
        let vehicleId = vehicle.id
        var sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            routeId: trip.routeId,
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            globalRepository: MockGlobalRepository(response: globalData),
            tripPredictionsRepository: tripPredictionsRepository,
            tripRepository: tripRepository,
            vehicleRepository: FakeVehicleRepository(response: .init(vehicle: vehicle))
        )

        let routeExp = sut.on(\.didLoadData) { view in
            let stop1Row = try view.find(TripDetailsStopView.self, containing: stop1.name)
            let stop2Row = try view.find(TripDetailsStopView.self, containing: stop2.name)
            XCTAssertNotNil(try stop1Row.find(RoutePill.self, containing: "Green Line"))
            XCTAssertThrowsError(try stop1Row.find(RoutePill.self, containing: "Red Line"))
            XCTAssertNotNil(try stop2Row.find(RoutePill.self, containing: "1"))
            XCTAssertThrowsError(try stop2Row.find(RoutePill.self, containing: "Red Line"))
        }

        ViewHosting.host(view: sut)

        wait(for: [routeExp], timeout: 5)
    }

    @MainActor func testMissingTrip() throws {
        let objects = ObjectCollectionBuilder()

        let tripId = "ADDED-trip"

        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = tripId
            vehicle.currentStatus = .inTransitTo
        }

        let globalData = GlobalResponse(objects: objects, patternIdsByStop: [:])

        let tripSchedulesLoaded = PassthroughSubject<Void, Never>()

        let tripRepository = FakeTripRepository(
            tripError: ApiResultError(code: 404, message: "Bad response"),
            scheduleResponse: TripSchedulesResponse.StopIds(stopIds: []),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let tripPredictionsLoaded = PassthroughSubject<Void, Never>()

        let tripPredictionsRepository = FakeTripPredictionsRepository(
            response: .init(objects: objects),
            onConnect: { _ in tripPredictionsLoaded.send() }
        )

        let vehicleId = vehicle.id
        let nearbyVM = NearbyViewModel(alertsRepository: MockAlertsRepository())
        nearbyVM.alerts = .init(alerts: [:])
        let sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            routeId: "Red",
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            globalRepository: MockGlobalRepository(response: globalData),
            tripPredictionsRepository: tripPredictionsRepository,
            tripRepository: tripRepository,
            vehicleRepository: FakeVehicleRepository(response: .init(vehicle: vehicle))
        )

        let everythingLoaded = tripSchedulesLoaded.zip(tripPredictionsLoaded)

        let routeExp = sut.inspection.inspect(onReceive: everythingLoaded, after: 0.1) { view in
            XCTAssertNotNil(try view.find(VehicleCardView.self))
            XCTAssertThrowsError(try view.find(TripDetailsStopView.self))
        }

        ViewHosting.host(view: sut)

        wait(for: [routeExp], timeout: 1)
    }

    func testBackButton() throws {
        let objects = ObjectCollectionBuilder()
        objects.stop { _ in }

        class FakeNearbyVM: NearbyViewModel {
            let backExp: XCTestExpectation
            init(_ backExp: XCTestExpectation) {
                self.backExp = backExp
                super.init()
            }

            override func goBack() {
                backExp.fulfill()
            }
        }

        let backExp = XCTestExpectation(description: "goBack called")

        let sut = TripDetailsPage(
            tripId: "tripId",
            vehicleId: "veicleId",
            routeId: "routeId",
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: FakeNearbyVM(backExp),
            mapVM: .init(),
            tripPredictionsRepository: FakeTripPredictionsRepository(response: .init(objects: objects)),
            tripRepository: FakeTripRepository(
                tripResponse: .init(trip: objects.trip { _ in }),
                scheduleResponse: TripSchedulesResponse.StopIds(stopIds: ["stop1"])
            ),
            vehicleRepository: FakeVehicleRepository(response: .init(vehicle: nil))
        )

        try sut.inspect().find(viewWithAccessibilityLabel: "Back").button().tap()

        wait(for: [backExp], timeout: 2)
    }

    func testCloseButton() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let nearbyVM = NearbyViewModel(
            navigationStack: [
                .legacyStopDetails(stop, nil),
                .legacyStopDetails(stop, nil),
                .legacyStopDetails(stop, nil),
            ]
        )

        let sut = TripDetailsPage(
            tripId: "tripId",
            vehicleId: "veicleId",
            routeId: "routeId",
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            tripPredictionsRepository: FakeTripPredictionsRepository(response: .init(objects: objects)),
            tripRepository: FakeTripRepository(
                tripResponse: .init(trip: objects.trip { _ in }),
                scheduleResponse: TripSchedulesResponse.StopIds(stopIds: ["stop1"])
            ),
            vehicleRepository: FakeVehicleRepository(response: .init(vehicle: nil))
        )

        try sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()

        XCTAssert(nearbyVM.navigationStack.isEmpty)
    }

    func testUpdatesMapVMSelectedTrip() throws {
        let objects = ObjectCollectionBuilder()

        let trip = objects.trip { _ in }

        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.currentStatus = .inTransitTo
        }

        let vehicleRepository = FakeVehicleRepository(response: .init(vehicle: vehicle))

        let mapVM = MapViewModel()

        let tripId = trip.id
        let vehicleId = vehicle.id
        let sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            routeId: trip.routeId,
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: mapVM,
            vehicleRepository: vehicleRepository
        )

        ViewHosting.host(view: sut)

        let selectedVehicleSetExp = expectation(description: "selected vehicle should be set")
        let subscription = mapVM.$selectedVehicle.drop(while: { $0 == nil }).sink {
            XCTAssertEqual($0, vehicle)
            selectedVehicleSetExp.fulfill()
        }

        wait(for: [selectedVehicleSetExp], timeout: 2)

        subscription.cancel()
    }

    @MainActor func testResolvesParentStop() {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let trip = objects.trip { $0.routeId = route.id }
        let vehicle = objects.vehicle { $0.currentStatus = .inTransitTo }
        let nearbyVM = NearbyViewModel()
        let parentStop = objects.stop { $0.childStopIds = ["child"] }
        let childStop = objects.stop {
            $0.id = "child"
            $0.parentStationId = parentStop.id
        }

        let tripLoaded = PassthroughSubject<Void, Never>()

        let analyticsExp = expectation(description: "sends analytics event")

        let sut = TripDetailsPage(
            tripId: trip.id,
            vehicleId: vehicle.id,
            routeId: route.id,
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            globalRepository: MockGlobalRepository(response: .init(objects: objects, patternIdsByStop: [:])),
            tripRepository: FakeTripRepository(
                tripResponse: .init(trip: trip),
                scheduleResponse: .Unknown.shared,
                onGetTrip: { tripLoaded.send() }
            ),
            analytics: MockAnalytics(onLogEvent: { name, parameters in
                XCTAssertEqual(name, "tapped_downstream_stop")
                XCTAssertEqual(
                    parameters,
                    [
                        "route_id": route.id,
                        "stop_id": childStop.id,
                        "trip_id": trip.id,
                        "connecting_route_id": "connectingRoute",
                    ]
                )
                analyticsExp.fulfill()
            })
        )

        ViewHosting.host(view: sut)

        sut.inspection.inspect(onReceive: tripLoaded, after: 1) { view in
            try view.actualView().onTapStop(
                entry: .legacyStopDetails(childStop, nil),
                stop: .init(
                    stop: childStop,
                    stopSequence: 1,
                    disruption: nil,
                    schedule: nil,
                    prediction: nil,
                    predictionStop: nil,
                    vehicle: nil,
                    routes: []
                ),
                connectingRouteId: "connectingRoute"
            )
            XCTAssertEqual(nearbyVM.navigationStack, [.legacyStopDetails(parentStop, nil)])
        }

        wait(for: [analyticsExp], timeout: 5)
    }

    func testLeavesAndJoinsPredictionsOnTripChange() throws {
        let objects = ObjectCollectionBuilder()
        objects.stop { _ in }

        let predictionsJoinExp = XCTestExpectation(description: "predictions joined")
        let predictionsLeaveExp = XCTestExpectation(description: "predictions left")

        let sut = TripDetailsPage(
            tripId: "tripId",
            vehicleId: "veicleId",
            routeId: "routeId",
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            tripPredictionsRepository: FakeTripPredictionsRepository(response: .init(objects: objects),
                                                                     onConnect: { _ in predictionsJoinExp.fulfill() },
                                                                     onDisconnect: { predictionsLeaveExp.fulfill() }),
            tripRepository: FakeTripRepository(
                tripResponse: .init(trip: objects.trip { _ in }),
                scheduleResponse: TripSchedulesResponse.StopIds(stopIds: ["stop1"])
            ),
            vehicleRepository: FakeVehicleRepository(response: .init(vehicle: nil))
        )

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: "newTripId", index: 0)

        wait(for: [predictionsLeaveExp, predictionsJoinExp], timeout: 2)
    }

    func testLeavesAndJoinsVehicleOnChange() throws {
        let objects = ObjectCollectionBuilder()
        objects.stop { _ in }

        let vehicleJoinExp = XCTestExpectation(description: "vehicle joined")
        let vehicleLeaveExp = XCTestExpectation(description: "vehicle left")

        let sut = TripDetailsPage(
            tripId: "tripId",
            vehicleId: "veicleId",
            routeId: "routeId",
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            tripPredictionsRepository: FakeTripPredictionsRepository(response: .init(objects: objects)),
            tripRepository: FakeTripRepository(
                tripResponse: .init(trip: objects.trip { _ in }),
                scheduleResponse: TripSchedulesResponse.StopIds(stopIds: ["stop1"])
            ),
            vehicleRepository: FakeVehicleRepository(
                response: .init(vehicle: nil),
                onConnect: { vehicleJoinExp.fulfill() },
                onDisconnect: { vehicleLeaveExp.fulfill() }
            )
        )

        // Index 1 because first onChange of a string is for tripId
        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: "newTripId", index: 1)

        wait(for: [vehicleLeaveExp, vehicleJoinExp], timeout: 2)
    }

    func testDebugModeNotShownByDefault() throws {
        let objects = ObjectCollectionBuilder()
        objects.stop { _ in }
        let sut = TripDetailsPage(
            tripId: "tripId",
            vehicleId: "vehicleId",
            routeId: "routeId",
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            tripPredictionsRepository: FakeTripPredictionsRepository(response: .init(objects: objects)),
            tripRepository: FakeTripRepository(
                tripResponse: .init(trip: objects.trip { _ in }),
                scheduleResponse: TripSchedulesResponse.StopIds(stopIds: [])
            ),
            vehicleRepository: FakeVehicleRepository(
                response: .init(vehicle: nil),
                onConnect: {},
                onDisconnect: {}
            )
        )
        ViewHosting.host(view: sut)
        XCTAssertThrowsError(try sut.inspect().find(text: "trip id: tripId"))
    }

    func testDebugModeShown() throws {
        let objects = ObjectCollectionBuilder()
        objects.stop { _ in }
        let sut = TripDetailsPage(
            tripId: "tripId",
            vehicleId: "vehicleId",
            routeId: "routeId",
            target: nil,
            errorBannerVM: .init(),
            nearbyVM: .init(showDebugMessages: true),
            mapVM: .init(),
            tripPredictionsRepository: FakeTripPredictionsRepository(response: .init(objects: objects)),
            tripRepository: FakeTripRepository(
                tripResponse: .init(trip: objects.trip { _ in }),
                scheduleResponse: TripSchedulesResponse.StopIds(stopIds: [])
            ),
            vehicleRepository: FakeVehicleRepository(
                response: .init(vehicle: nil),
                onConnect: {},
                onDisconnect: {}
            )
        )
        ViewHosting.host(view: sut)
        XCTAssertNotNil(try sut.inspect().find(text: "trip id: tripId"))
    }

    class FakeTripRepository: IdleTripRepository {
        let tripResponse: ApiResult<TripResponse>
        let scheduleResponse: TripSchedulesResponse
        let onGetTrip: (() -> Void)?
        let onGetTripSchedules: (() -> Void)?

        init(
            tripResponse: TripResponse,
            scheduleResponse: TripSchedulesResponse,
            onGetTrip: (() -> Void)? = nil,
            onGetTripSchedules: (() -> Void)? = nil
        ) {
            self.tripResponse = ApiResultOk(data: tripResponse)
            self.scheduleResponse = scheduleResponse
            self.onGetTrip = onGetTrip
            self.onGetTripSchedules = onGetTripSchedules
        }

        init(
            tripError: ApiResultError<TripResponse>,
            scheduleResponse: TripSchedulesResponse,
            onGetTrip: (() -> Void)? = nil,
            onGetTripSchedules: (() -> Void)? = nil
        ) {
            tripResponse = tripError
            self.scheduleResponse = scheduleResponse
            self.onGetTrip = onGetTrip
            self.onGetTripSchedules = onGetTripSchedules
        }

        override func __getTrip(tripId _: String) async throws -> ApiResult<TripResponse> {
            onGetTrip?()
            return tripResponse
        }

        override func __getTripSchedules(tripId _: String) async throws -> ApiResult<TripSchedulesResponse> {
            onGetTripSchedules?()
            return ApiResultOk(data: scheduleResponse)
        }
    }

    class FakeTripPredictionsRepository: ITripPredictionsRepository {
        let response: PredictionsStreamDataResponse?
        let onConnect: ((_ tripId: String) -> Void)?
        let onDisconnect: (() -> Void)?

        init(response: PredictionsStreamDataResponse?,
             onConnect: ((_ tripId: String) -> Void)? = nil,
             onDisconnect: (() -> Void)? = nil) {
            self.response = response
            self.onConnect = onConnect
            self.onDisconnect = onDisconnect
        }

        func connect(
            tripId: String,
            onReceive: @escaping (ApiResult<PredictionsStreamDataResponse>) -> Void
        ) {
            onConnect?(tripId)
            let result = if let response {
                ApiResultOk(data: response)
            } else {
                ApiResultError<PredictionsStreamDataResponse>(code: 404, message: "Failed to load predictions")
            }
            onReceive(result)
        }

        var lastUpdated: Instant?

        func shouldForgetPredictions(predictionCount _: Int32) -> Bool {
            false
        }

        func disconnect() {
            onDisconnect?()
        }
    }

    class FakeVehicleRepository: IVehicleRepository {
        let response: VehicleStreamDataResponse?
        let onConnect: (() -> Void)?
        let onDisconnect: (() -> Void)?

        init(response: VehicleStreamDataResponse?,
             onConnect: (() -> Void)? = nil,
             onDisconnect: (() -> Void)? = nil) {
            self.response = response
            self.onConnect = onConnect
            self.onDisconnect = onDisconnect
        }

        func connect(
            vehicleId _: String,
            onReceive: @escaping (ApiResult<VehicleStreamDataResponse>) -> Void
        ) {
            if let response {
                onReceive(ApiResultOk(data: response))
            }
            onConnect?()
        }

        func disconnect() {
            onDisconnect?()
        }
    }
}

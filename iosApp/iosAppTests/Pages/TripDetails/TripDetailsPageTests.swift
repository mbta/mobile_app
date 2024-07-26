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
    func testLoadsStopList() throws {
        let objects = ObjectCollectionBuilder()

        let stop1 = objects.stop { stop in
            stop.name = "Somewhere"
        }
        let stop2 = objects.stop { stop in
            stop.name = "Elsewhere"
        }

        objects.prediction { prediction in
            prediction.stopId = stop2.id
            prediction.stopSequence = 2
            prediction.departureTime = Date.now.addingTimeInterval(30).toKotlinInstant()
        }

        let tripSchedulesLoaded = PassthroughSubject<Void, Never>()

        let tripRepository = FakeTripRepository(
            response: TripSchedulesResponse.StopIds(stopIds: [stop1.id, stop2.id]),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let tripPredictionsRepository = FakeTripPredictionsRepository(response: .init(objects: objects))

        let tripId = "123"
        let vehicleId = "999"
        let sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            target: nil,
            nearbyVM: .init(),
            mapVM: .init(),
            globalRepository: FakeGlobalRepository(response: .init(objects: objects, patternIdsByStop: [:])),
            tripPredictionsRepository: tripPredictionsRepository,
            tripRepository: tripRepository
        )

        let showsStopsExp = sut.inspection.inspect(onReceive: tripSchedulesLoaded, after: 1) { view in
            XCTAssertNotNil(try view.find(text: "Somewhere"))
            XCTAssertNotNil(try view.find(text: "Elsewhere"))
            XCTAssertNotNil(try view.find(text: "Elsewhere").parent().find(text: "ARR"))
        }

        ViewHosting.host(view: sut)

        wait(for: [showsStopsExp], timeout: 2)
    }

    func testIncludesVehicleCard() throws {
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
            response: TripSchedulesResponse.StopIds(stopIds: [stop1.id]),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let tripPredictionsRepository = FakeTripPredictionsRepository(response: .init(objects: objects))

        let tripId = trip.id
        let vehicleId = vehicle.id
        let sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            target: nil,
            nearbyVM: .init(),
            mapVM: .init(),
            globalRepository: FakeGlobalRepository(response: .init(objects: objects, patternIdsByStop: [:])),
            tripPredictionsRepository: tripPredictionsRepository,
            tripRepository: tripRepository,
            vehicleRepository: FakeVehicleRepository(response: .init(vehicle: vehicle))
        )

        let showVehicleCardExp = sut.inspection.inspect(onReceive: tripSchedulesLoaded, after: 1) { view in
            XCTAssertNotNil(try view.find(VehicleOnTripView.self))
        }

        ViewHosting.host(view: sut)

        wait(for: [showVehicleCardExp], timeout: 5)
    }

    func testSplitsWithTarget() throws {
        let objects = ObjectCollectionBuilder()

        let stop1 = objects.stop { stop in
            stop.name = "Somewhere"
        }
        let stop2 = objects.stop { stop in
            stop.name = "Elsewhere"
        }

        let tripSchedulesLoaded = PassthroughSubject<Void, Never>()

        let tripRepository = FakeTripRepository(
            response: TripSchedulesResponse.StopIds(stopIds: [stop1.id, stop2.id]),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let tripId = "123"
        let vehicleId = "999"
        let sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            target: .init(stopId: stop1.id, stopSequence: 998),
            nearbyVM: .init(),
            mapVM: .init(),
            globalRepository: FakeGlobalRepository(response: .init(objects: objects, patternIdsByStop: [:])),
            tripPredictionsRepository: FakeTripPredictionsRepository(response: .init(objects: objects)),
            tripRepository: tripRepository,
            vehicleRepository: FakeVehicleRepository(response: nil)
        )

        let splitViewExp = sut.inspection.inspect(onReceive: tripSchedulesLoaded, after: 1) { view in
            XCTAssertNotNil(try view.find(TripDetailsStopListSplitView.self))
        }

        ViewHosting.host(view: sut)

        wait(for: [splitViewExp], timeout: 2)
    }

    func testDisplaysTransferRoutes() throws {
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

        let tripSchedulesLoaded = PassthroughSubject<Void, Never>()

        let tripRepository = FakeTripRepository(
            response: TripSchedulesResponse.StopIds(stopIds: [stop1.id, stop2.id]),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let tripPredictionsLoaded = PassthroughSubject<Void, Never>()

        let tripPredictionsRepository = FakeTripPredictionsRepository(
            response: .init(objects: objects),
            onConnect: { _ in tripPredictionsLoaded.send() }
        )

        let tripId = trip.id
        let vehicleId = vehicle.id
        let sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            target: nil,
            nearbyVM: .init(),
            mapVM: .init(),
            globalRepository: FakeGlobalRepository(response: globalData),
            tripPredictionsRepository: tripPredictionsRepository,
            tripRepository: tripRepository,
            vehicleRepository: FakeVehicleRepository(response: .init(vehicle: vehicle))
        )

        let everythingLoaded = tripSchedulesLoaded.zip(tripPredictionsLoaded)

        let routeExp = sut.inspection.inspect(onReceive: everythingLoaded, after: 1) { view in
            let stop1Row = try view.find(TripDetailsStopView.self, containing: stop1.name)
            let stop2Row = try view.find(TripDetailsStopView.self, containing: stop2.name)
            XCTAssertNotNil(try stop1Row.find(RoutePill.self, containing: "Green Line"))
            XCTAssertThrowsError(try stop1Row.find(RoutePill.self, containing: "Red Line"))
            XCTAssertNotNil(try stop2Row.find(RoutePill.self, containing: "1"))
            XCTAssertThrowsError(try stop2Row.find(RoutePill.self, containing: "Red Line"))
        }

        ViewHosting.host(view: sut)

        wait(for: [routeExp], timeout: 2)
    }

    func testBackButton() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

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
            target: nil,
            nearbyVM: FakeNearbyVM(backExp),
            mapVM: .init(),
            tripPredictionsRepository: FakeTripPredictionsRepository(response: .init(objects: objects)),
            tripRepository: FakeTripRepository(response: TripSchedulesResponse
                .StopIds(stopIds: ["stop1"])),
            vehicleRepository: FakeVehicleRepository(response: .init(vehicle: nil))
        )

        try sut.inspect().find(ActionButton.self).button().tap()

        wait(for: [backExp], timeout: 2)
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
            target: nil,
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

    class FakeGlobalRepository: IGlobalRepository {
        let response: GlobalResponse

        init(response: GlobalResponse) {
            self.response = response
        }

        func __getGlobalData() async throws -> GlobalResponse {
            response
        }
    }

    class FakeTripRepository: IdleTripRepository {
        let response: TripSchedulesResponse
        let onGetTripSchedules: (() -> Void)?

        init(response: TripSchedulesResponse, onGetTripSchedules: (() -> Void)? = nil) {
            self.response = response
            self.onGetTripSchedules = onGetTripSchedules
        }

        override func __getTripSchedules(tripId _: String) async throws -> TripSchedulesResponse {
            onGetTripSchedules?()
            return response
        }
    }

    class FakeTripPredictionsRepository: ITripPredictionsRepository {
        let response: PredictionsStreamDataResponse
        let onConnect: ((_ tripId: String) -> Void)?

        init(response: PredictionsStreamDataResponse, onConnect: ((_ tripId: String) -> Void)? = nil) {
            self.response = response
            self.onConnect = onConnect
        }

        func connect(
            tripId: String,
            onReceive: @escaping (Outcome<PredictionsStreamDataResponse, __SocketError>) -> Void
        ) {
            onConnect?(tripId)
            onReceive(.init(data: response, error: nil))
        }

        func disconnect() {}
    }

    class FakeVehicleRepository: IVehicleRepository {
        let response: VehicleStreamDataResponse?
        init(response: VehicleStreamDataResponse?) {
            self.response = response
        }

        func connect(
            vehicleId _: String,
            onReceive: @escaping (Outcome<VehicleStreamDataResponse, __SocketError>) -> Void
        ) {
            onReceive(.init(data: response, error: nil))
        }

        func disconnect() {}
    }
}

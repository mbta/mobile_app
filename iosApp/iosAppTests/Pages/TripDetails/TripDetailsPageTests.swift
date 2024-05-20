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

        let globalFetcher = GlobalFetcher(backend: IdleBackend())
        globalFetcher.response = .init(objects: objects, patternIdsByStop: [:])

        let tripSchedulesLoaded = PassthroughSubject<Void, Never>()

        let tripSchedulesRepository = FakeTripSchedulesRepository(
            response: TripSchedulesResponse.StopIds(stopIds: [stop1.id, stop2.id]),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let tripPredictionsFetcher = FakeTripPredictionsFetcher(response: .init(objects: objects))

        let tripId = "123"
        let vehicleId = "999"
        let sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            target: nil,
            globalFetcher: globalFetcher,
            tripPredictionsFetcher: tripPredictionsFetcher,
            tripSchedulesRepository: tripSchedulesRepository,
            vehicleFetcher: .init(socket: MockSocket())
        )

        let showsStopsExp = sut.inspection.inspect(onReceive: tripSchedulesLoaded, after: 0.1) { view in
            XCTAssertNotNil(try view.find(text: "Somewhere"))
            XCTAssertNotNil(try view.find(text: "Elsewhere"))
            XCTAssertNotNil(try view.find(text: "Elsewhere").parent().find(text: "ARR"))
        }

        ViewHosting.host(view: sut)

        wait(for: [showsStopsExp], timeout: 1)
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
            prediction.stopSequence = 1
            prediction.departureTime = Date.now.toKotlinInstant()
        }

        let response = GlobalResponse(objects: objects, patternIdsByStop: [:])
        let globalFetcher = GlobalFetcher(backend: IdleBackend(), stops: response.stops, routes: response.routes)
        globalFetcher.response = response

        let tripSchedulesLoaded = PassthroughSubject<Void, Never>()

        let tripSchedulesRepository = FakeTripSchedulesRepository(
            response: TripSchedulesResponse.StopIds(stopIds: [stop1.id]),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let tripPredictionsFetcher = FakeTripPredictionsFetcher(response: .init(objects: objects))

        let tripId = trip.id
        let vehicleId = vehicle.id
        let sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            target: nil,
            globalFetcher: globalFetcher,
            tripPredictionsFetcher: tripPredictionsFetcher,
            tripSchedulesRepository: tripSchedulesRepository,
            vehicleFetcher: FakeVehicleFetcher(response: .init(vehicle: vehicle))
        )

        let showVehicleCardExp = sut.inspection.inspect(onReceive: tripSchedulesLoaded, after: 0.1) { view in
            XCTAssertNotNil(try view.find(VehicleOnTripView.self))
        }

        ViewHosting.host(view: sut)

        wait(for: [showVehicleCardExp], timeout: 2)
    }

    func testSplitsWithTarget() throws {
        let objects = ObjectCollectionBuilder()

        let stop1 = objects.stop { stop in
            stop.name = "Somewhere"
        }
        let stop2 = objects.stop { stop in
            stop.name = "Elsewhere"
        }

        let globalFetcher = GlobalFetcher(backend: IdleBackend())
        globalFetcher.response = .init(objects: objects, patternIdsByStop: [:])

        let tripSchedulesLoaded = PassthroughSubject<Void, Never>()

        let tripSchedulesRepository = FakeTripSchedulesRepository(
            response: TripSchedulesResponse.StopIds(stopIds: [stop1.id, stop2.id]),
            onGetTripSchedules: { tripSchedulesLoaded.send() }
        )

        let tripId = "123"
        let vehicleId = "999"
        let sut = TripDetailsPage(
            tripId: tripId,
            vehicleId: vehicleId,
            target: .init(stopId: stop1.id, stopSequence: 998),
            globalFetcher: globalFetcher,
            tripPredictionsFetcher: FakeTripPredictionsFetcher(response: .init(objects: objects)),
            tripSchedulesRepository: tripSchedulesRepository,
            vehicleFetcher: FakeVehicleFetcher(response: nil)
        )

        let splitViewExp = sut.inspection.inspect(onReceive: tripSchedulesLoaded, after: 0.1) { view in
            XCTAssertNotNil(try view.find(TripDetailsStopListSplitView.self))
        }

        ViewHosting.host(view: sut)

        wait(for: [splitViewExp], timeout: 1)
    }

    class FakeTripSchedulesRepository: ITripSchedulesRepository {
        let response: TripSchedulesResponse
        let onGetTripSchedules: (() -> Void)?

        init(response: TripSchedulesResponse, onGetTripSchedules: (() -> Void)? = nil) {
            self.response = response
            self.onGetTripSchedules = onGetTripSchedules
        }

        func __getTripSchedules(tripId _: String) async throws -> TripSchedulesResponse {
            onGetTripSchedules?()
            return response
        }
    }

    class FakeTripPredictionsFetcher: TripPredictionsFetcher {
        let response: PredictionsStreamDataResponse
        let onRun: ((_ tripId: String) -> Void)?

        init(response: PredictionsStreamDataResponse, onRun: ((_ tripId: String) -> Void)? = nil) {
            self.response = response
            self.onRun = onRun
            super.init(socket: MockSocket())
        }

        override func run(tripId: String) {
            onRun?(tripId)
            predictions = response
        }
    }

    class FakeVehicleFetcher: VehicleFetcher {
        let overriddenResponse: VehicleStreamDataResponse?
        init(response: VehicleStreamDataResponse?) {
            overriddenResponse = response
            super.init(socket: MockSocket())
        }

        override func run(vehicleId _: String) {
            response = overriddenResponse
        }
    }
}

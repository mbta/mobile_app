//
//  StopDetailsViewModelTests.swift
//  iosAppTests
//
//  Created by esimon on 12/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
import XCTest

final class StopDetailsViewModelTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testLoadGlobalData() async throws {
        let global = GlobalResponse(objects: ObjectCollectionBuilder())
        let exp = expectation(description: "global data is fetched")
        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: global, onGet: { exp.fulfill() })
        )
        Task { await stopDetailsVM.activateGlobalListener() }
        _ = try await stopDetailsVM.globalRepository.getGlobalData()
        await fulfillment(of: [exp], timeout: 1)
        try await Task.sleep(for: .seconds(1))
        XCTAssertEqual(stopDetailsVM.global, global)
    }

    func testLoadPredictions() async throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let predictions = PredictionsByStopJoinResponse(objects: objects)
        let connectExp = expectation(description: "predictions are connected")
        let successExp = expectation(description: "prediction success callback was called")
        let completeExp = expectation(description: "prediction complete callback was called")
        let disconnectExp = expectation(description: "predictions are disconnected")
        let stopDetailsVM = StopDetailsViewModel(
            predictionsRepository: MockPredictionsRepository(
                onConnectV2: { _ in connectExp.fulfill() },
                onDisconnect: { disconnectExp.fulfill() },
                connectV2Response: predictions
            )
        )

        stopDetailsVM.joinPredictions(
            stop.id,
            onSuccess: { successExp.fulfill() },
            onComplete: { completeExp.fulfill() }
        )
        await fulfillment(of: [connectExp, successExp, completeExp], timeout: 2)
        XCTAssertEqual(stopDetailsVM.predictionsByStop, predictions)
        stopDetailsVM.leavePredictions()
        await fulfillment(of: [disconnectExp], timeout: 1)
    }

    func testGetDepartures() async throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()

        let headsign0 = "0"
        let pattern0 = objects.routePattern(route: route) { pattern in
            pattern.directionId = 0
            pattern.representativeTrip { trip in trip.headsign = headsign0 }
            pattern.typicality = .typical
        }
        let trip0 = objects.trip(routePattern: pattern0) { trip in trip.headsign = headsign0 }
        let upcoming0 = objects.upcomingTrip(schedule: objects.schedule { schedule in
            schedule.trip = trip0
            schedule.routeId = route.id
            schedule.stopId = stop.id
            schedule.departureTime = (now + 10 * 60).toKotlinInstant()
        })
        let direction0 = Direction(
            name: (route.directionNames[0] as? String)!,
            destination: (route.directionDestinations[0] as? String)!,
            id: 0
        )

        let headsign1 = "1"
        let pattern1 = objects.routePattern(route: route) { pattern in
            pattern.directionId = 1
            pattern.representativeTrip { trip in trip.headsign = headsign1 }
            pattern.typicality = .typical
        }
        let trip1 = objects.trip(routePattern: pattern1) { trip in trip.headsign = headsign1 }
        let schedule1 = objects.schedule { schedule in
            schedule.trip = trip1
            schedule.routeId = route.id
            schedule.stopId = stop.id
            schedule.departureTime = (now + 10 * 60).toKotlinInstant()
        }
        let upcoming1 = objects.upcomingTrip(
            schedule: schedule1,
            prediction: objects.prediction(schedule: schedule1) { prediction in
                prediction.trip = trip1
                prediction.routeId = route.id
                prediction.stopId = stop.id
                prediction.departureTime = (now + 10 * 60).toKotlinInstant()
            }
        )
        let direction1 = Direction(
            name: (route.directionNames[1] as? String)!,
            destination: (route.directionDestinations[1] as? String)!,
            id: 1
        )

        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.global = .init(objects: objects, patternIdsByStop: [stop.id: [pattern0.id, pattern1.id]])
        stopDetailsVM.predictionsByStop = .init(objects: objects)
        stopDetailsVM.schedulesResponse = .init(objects: objects)

        let departures = stopDetailsVM.getDepartures(
            stopId: stop.id,
            alerts: .init(objects: objects),
            useTripHeadsigns: false,
            now: now
        )

        XCTAssertEqual(StopDetailsDepartures(routes: [.init(
            routes: [route],
            line: nil,
            stop: stop,
            patterns: [
                .ByHeadsign(
                    route: route, headsign: trip0.headsign, line: nil,
                    patterns: [pattern0], upcomingTrips: [upcoming0]
                ),
                .ByHeadsign(
                    route: route, headsign: trip1.headsign, line: nil,
                    patterns: [pattern1], upcomingTrips: [upcoming1]
                ),
            ],
            directions: [direction0, direction1]
        )]), departures)
    }

    func testLoadTripData() async throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()

        let pattern = objects.routePattern(route: route) { pattern in
            pattern.directionId = 0
            pattern.representativeTrip { trip in trip.headsign = "0" }
            pattern.typicality = .typical
        }
        let trip = objects.trip(routePattern: pattern) { trip in trip.headsign = "0" }
        let schedule = objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id
            schedule.stopId = stop.id
        }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = stop.id
            vehicle.currentStopSequence = 0
        }

        let tripPredictionConnectExp = expectation(description: "connected to trip prediction channel")
        let tripPredictionDisconnectExp = expectation(description: "disconnected from trip prediction channel")
        tripPredictionDisconnectExp.expectedFulfillmentCount = 3

        let vehicleConnectExp = expectation(description: "connected to vehicle channel")
        let vehicleDisconnectExp = expectation(description: "disconnected from vehicle channel")
        vehicleDisconnectExp.expectedFulfillmentCount = 3

        let tripPredictions = PredictionsStreamDataResponse(objects: objects)
        let tripSchedules = TripSchedulesResponse.Schedules(schedules: [schedule])
        let stopDetailsVM = StopDetailsViewModel(
            tripPredictionsRepository: MockTripPredictionsRepository(
                onConnect: { tripPredictionConnectExp.fulfill() },
                onDisconnect: { tripPredictionDisconnectExp.fulfill() },
                response: tripPredictions
            ),
            tripRepository: MockTripRepository(
                tripSchedulesResponse: tripSchedules,
                tripResponse: .init(trip: trip)
            ),
            vehicleRepository: MockVehicleRepository(
                onConnect: { vehicleConnectExp.fulfill() },
                onDisconnect: { vehicleDisconnectExp.fulfill() },
                outcome: ApiResultOk(data: .init(vehicle: vehicle))
            )
        )

        let tripFilter = TripDetailsFilter(
            tripId: trip.id,
            vehicleId: vehicle.id,
            stopSequence: 0,
            selectionLock: false
        )

        XCTAssertNil(stopDetailsVM.tripData)
        await stopDetailsVM.handleTripFilterChange(tripFilter)
        await fulfillment(of: [tripPredictionConnectExp, vehicleConnectExp], timeout: 1)
        XCTAssertEqual(stopDetailsVM.tripData?.tripFilter, tripFilter)
        XCTAssertEqual(stopDetailsVM.tripData?.trip, trip)
        XCTAssertEqual(stopDetailsVM.tripData?.tripSchedules, tripSchedules)
        XCTAssertEqual(stopDetailsVM.tripData?.tripPredictions, tripPredictions)
        XCTAssertEqual(stopDetailsVM.tripData?.vehicle, vehicle)
        await stopDetailsVM.clearTripDetails()
        XCTAssertNil(stopDetailsVM.tripData)
        await fulfillment(of: [tripPredictionDisconnectExp, vehicleDisconnectExp], timeout: 1)
    }

    func testSkipLoadingTripData() async throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()

        let pattern = objects.routePattern(route: route) { pattern in
            pattern.directionId = 0
            pattern.representativeTrip { trip in trip.headsign = "0" }
            pattern.typicality = .typical
        }
        let trip = objects.trip(routePattern: pattern) { trip in trip.headsign = "0" }
        let schedule = objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id
            schedule.stopId = stop.id
        }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = stop.id
            vehicle.currentStopSequence = 0
        }

        let tripPredictionConnectExp = expectation(description: "connected to trip prediction channel")
        tripPredictionConnectExp.isInverted = true
        let tripPredictionDisconnectExp = expectation(description: "disconnected from trip prediction channel")
        tripPredictionDisconnectExp.isInverted = true

        let vehicleConnectExp = expectation(description: "connected to vehicle channel")
        vehicleConnectExp.isInverted = true
        let vehicleDisconnectExp = expectation(description: "disconnected from vehicle channel")
        vehicleDisconnectExp.isInverted = true

        let tripPredictions = PredictionsStreamDataResponse(objects: objects)
        let tripSchedules = TripSchedulesResponse.Schedules(schedules: [schedule])
        let stopDetailsVM = StopDetailsViewModel(
            tripPredictionsRepository: MockTripPredictionsRepository(
                onConnect: { tripPredictionConnectExp.fulfill() },
                onDisconnect: { tripPredictionDisconnectExp.fulfill() },
                response: tripPredictions
            ),
            tripRepository: MockTripRepository(
                tripSchedulesResponse: tripSchedules,
                tripResponse: .init(trip: trip)
            ),
            vehicleRepository: MockVehicleRepository(
                onConnect: { vehicleConnectExp.fulfill() },
                onDisconnect: { vehicleDisconnectExp.fulfill() },
                outcome: ApiResultOk(data: .init(vehicle: vehicle))
            )
        )

        let tripFilter = TripDetailsFilter(
            tripId: trip.id,
            vehicleId: vehicle.id,
            stopSequence: 0,
            selectionLock: false
        )

        let tripData = TripData(
            tripFilter: tripFilter,
            trip: trip,
            tripSchedules: tripSchedules,
            tripPredictions: tripPredictions,
            vehicle: vehicle
        )

        stopDetailsVM.tripData = tripData

        let newTripFilter = TripDetailsFilter(
            tripId: trip.id,
            vehicleId: vehicle.id,
            stopSequence: 1,
            selectionLock: false
        )
        await stopDetailsVM.handleTripFilterChange(newTripFilter)
        XCTAssertEqual(stopDetailsVM.tripData?.tripFilter, newTripFilter)
        XCTAssertEqual(stopDetailsVM.tripData?.trip, trip)
        XCTAssertEqual(stopDetailsVM.tripData?.tripSchedules, tripSchedules)
        XCTAssertEqual(stopDetailsVM.tripData?.tripPredictions, tripPredictions)
        XCTAssertEqual(stopDetailsVM.tripData?.vehicle, vehicle)

        await fulfillment(of: [
            tripPredictionConnectExp,
            vehicleConnectExp,
            tripPredictionDisconnectExp,
            vehicleDisconnectExp,
        ], timeout: 1)
    }

    func testSkipLoadingRedundantVehicle() async throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()

        let pattern = objects.routePattern(route: route) { pattern in
            pattern.directionId = 0
            pattern.typicality = .typical
        }
        let trip0 = objects.trip(routePattern: pattern) { _ in }
        let trip1 = objects.trip(routePattern: pattern) { _ in }

        let schedule = objects.schedule { schedule in
            schedule.trip = trip0
            schedule.routeId = route.id
            schedule.stopId = stop.id
        }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip0.id
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = stop.id
            vehicle.currentStopSequence = 0
        }

        let tripPredictionConnectExp = expectation(description: "connected to trip prediction channel")
        let tripPredictionDisconnectExp = expectation(description: "disconnected from trip prediction channel")
        tripPredictionDisconnectExp.expectedFulfillmentCount = 2

        let vehicleConnectExp = expectation(description: "connected to vehicle channel")
        vehicleConnectExp.isInverted = true
        let vehicleDisconnectExp = expectation(description: "disconnected from vehicle channel")
        vehicleDisconnectExp.isInverted = true

        let tripPredictions = PredictionsStreamDataResponse(objects: objects)
        let tripSchedules = TripSchedulesResponse.Schedules(schedules: [schedule])
        let stopDetailsVM = StopDetailsViewModel(
            tripPredictionsRepository: MockTripPredictionsRepository(
                onConnect: { tripPredictionConnectExp.fulfill() },
                onDisconnect: { tripPredictionDisconnectExp.fulfill() },
                response: tripPredictions
            ),
            tripRepository: MockTripRepository(
                tripSchedulesResponse: tripSchedules,
                tripResponse: .init(trip: trip1)
            ),
            vehicleRepository: MockVehicleRepository(
                onConnect: { vehicleConnectExp.fulfill() },
                onDisconnect: { vehicleDisconnectExp.fulfill() },
                outcome: ApiResultOk(data: .init(vehicle: vehicle))
            )
        )

        let tripFilter = TripDetailsFilter(
            tripId: trip0.id,
            vehicleId: vehicle.id,
            stopSequence: 0,
            selectionLock: false
        )

        let tripData = TripData(
            tripFilter: tripFilter,
            trip: trip0,
            tripSchedules: TripSchedulesResponse.Unknown(),
            tripPredictions: .init(predictions: [:], trips: [:], vehicles: [:]),
            vehicle: vehicle
        )

        stopDetailsVM.tripData = tripData

        let newTripFilter = TripDetailsFilter(
            tripId: trip1.id,
            vehicleId: vehicle.id,
            stopSequence: 0,
            selectionLock: false
        )
        await stopDetailsVM.handleTripFilterChange(newTripFilter)
        try await Task.sleep(for: .seconds(1))
        XCTAssertEqual(stopDetailsVM.tripData?.tripFilter, newTripFilter)
        XCTAssertEqual(stopDetailsVM.tripData?.trip, trip1)
        XCTAssertEqual(stopDetailsVM.tripData?.tripSchedules, tripSchedules)
        XCTAssertEqual(stopDetailsVM.tripData?.tripPredictions, tripPredictions)
        XCTAssertEqual(stopDetailsVM.tripData?.vehicle, vehicle)

        await fulfillment(of: [
            tripPredictionConnectExp,
            vehicleConnectExp,
            tripPredictionDisconnectExp,
            vehicleDisconnectExp,
        ], timeout: 1)
    }

    func testSkipLoadingRedundantTrip() async throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()

        let pattern = objects.routePattern(route: route) { pattern in
            pattern.directionId = 0
            pattern.typicality = .typical
        }
        let trip = objects.trip(routePattern: pattern) { _ in }

        let schedule = objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id
            schedule.stopId = stop.id
        }
        let vehicle0 = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = stop.id
            vehicle.currentStopSequence = 0
        }
        let vehicle1 = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = stop.id
            vehicle.currentStopSequence = 0
        }

        let tripPredictionConnectExp = expectation(description: "connected to trip prediction channel")
        tripPredictionConnectExp.isInverted = true
        let tripPredictionDisconnectExp = expectation(description: "disconnected from trip prediction channel")
        tripPredictionDisconnectExp.isInverted = true

        let vehicleConnectExp = expectation(description: "connected to vehicle channel")
        let vehicleDisconnectExp = expectation(description: "disconnected from vehicle channel")
        vehicleDisconnectExp.expectedFulfillmentCount = 2

        let tripPredictions = PredictionsStreamDataResponse(objects: objects)
        let tripSchedules = TripSchedulesResponse.Schedules(schedules: [schedule])
        let stopDetailsVM = StopDetailsViewModel(
            tripPredictionsRepository: MockTripPredictionsRepository(
                onConnect: { tripPredictionConnectExp.fulfill() },
                onDisconnect: { tripPredictionDisconnectExp.fulfill() },
                response: tripPredictions
            ),
            tripRepository: MockTripRepository(
                tripSchedulesResponse: tripSchedules,
                tripResponse: .init(trip: trip)
            ),
            vehicleRepository: MockVehicleRepository(
                onConnect: { vehicleConnectExp.fulfill() },
                onDisconnect: { vehicleDisconnectExp.fulfill() },
                outcome: ApiResultOk(data: .init(vehicle: vehicle1))
            )
        )

        let tripFilter = TripDetailsFilter(
            tripId: trip.id,
            vehicleId: vehicle0.id,
            stopSequence: 0,
            selectionLock: false
        )

        let tripData = TripData(
            tripFilter: tripFilter,
            trip: trip,
            tripSchedules: tripSchedules,
            tripPredictions: tripPredictions,
            vehicle: vehicle0
        )

        stopDetailsVM.tripData = tripData

        let newTripFilter = TripDetailsFilter(
            tripId: trip.id,
            vehicleId: vehicle1.id,
            stopSequence: 0,
            selectionLock: false
        )
        await stopDetailsVM.handleTripFilterChange(newTripFilter)
        try await Task.sleep(for: .seconds(1))
        XCTAssertEqual(stopDetailsVM.tripData?.tripFilter, newTripFilter)
        XCTAssertEqual(stopDetailsVM.tripData?.trip, trip)
        XCTAssertEqual(stopDetailsVM.tripData?.tripSchedules, tripSchedules)
        XCTAssertEqual(stopDetailsVM.tripData?.tripPredictions, tripPredictions)
        XCTAssertEqual(stopDetailsVM.tripData?.vehicle, vehicle1)

        await fulfillment(of: [
            tripPredictionConnectExp,
            vehicleConnectExp,
            tripPredictionDisconnectExp,
            vehicleDisconnectExp,
        ], timeout: 1)
    }

    func testNilTripFilter() async throws {
        let objects = ObjectCollectionBuilder()
        let trip = objects.trip { _ in }

        let tripPredictionConnectExp = expectation(description: "connected to trip prediction channel")
        tripPredictionConnectExp.isInverted = true
        let tripPredictionDisconnectExp = expectation(description: "disconnected from trip prediction channel")

        let vehicleConnectExp = expectation(description: "connected to vehicle channel")
        vehicleConnectExp.isInverted = true
        let vehicleDisconnectExp = expectation(description: "disconnected from vehicle channel")

        let stopDetailsVM = StopDetailsViewModel(
            tripPredictionsRepository: MockTripPredictionsRepository(
                onConnect: { tripPredictionConnectExp.fulfill() },
                onDisconnect: { tripPredictionDisconnectExp.fulfill() }
            ),
            vehicleRepository: MockVehicleRepository(
                onConnect: { vehicleConnectExp.fulfill() },
                onDisconnect: { vehicleDisconnectExp.fulfill() }
            )
        )

        let tripData = TripData(
            tripFilter: TripDetailsFilter(
                tripId: "",
                vehicleId: "",
                stopSequence: 0,
                selectionLock: false
            ),
            trip: trip,
            tripSchedules: TripSchedulesResponse.Unknown(),
            tripPredictions: .init(objects: objects),
            vehicle: nil
        )

        stopDetailsVM.tripData = tripData

        await stopDetailsVM.handleTripFilterChange(nil)
        try await Task.sleep(for: .seconds(1))
        XCTAssertNil(stopDetailsVM.tripData)

        await fulfillment(of: [
            tripPredictionConnectExp,
            vehicleConnectExp,
            tripPredictionDisconnectExp,
            vehicleDisconnectExp,
        ], timeout: 1)
    }
}

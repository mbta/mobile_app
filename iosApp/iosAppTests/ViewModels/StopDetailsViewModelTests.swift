//
//  StopDetailsViewModelTests.swift
//  iosAppTests
//
//  Created by esimon on 12/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import Foundation
@testable import iosApp
import Shared
import XCTest

final class StopDetailsViewModelTests: XCTestCase {
    private var cancellables = Set<AnyCancellable>()

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testLoadGlobalData() async throws {
        let global = GlobalResponse(objects: ObjectCollectionBuilder())
        let exp = expectation(description: "global data is fetched")
        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: global, onGet: { exp.fulfill() })
        )
        _ = stopDetailsVM.loadGlobalData()
        await fulfillment(of: [exp], timeout: 1)
        try await Task.sleep(for: .seconds(1))
        XCTAssertEqual(stopDetailsVM.global, global)
    }

    func testHandleStopChange() async throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let leaveExpectation = expectation(description: "leaves predictions")
        let joinExpectation = expectation(description: "joins predictions")

        let predictionsRepo = MockPredictionsRepository(
            onConnect: {},
            onConnectV2: { _ in joinExpectation.fulfill() },
            onDisconnect: { leaveExpectation.fulfill() },
            connectOutcome: nil,
            connectV2Outcome: ApiResultOk(data: .init(objects: objects))
        )

        let scheduleExpectation = expectation(description: "schedules loaded")

        let stopDetailsVM = StopDetailsViewModel(
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(
                scheduleResponse: .init(objects: objects),
                callback: { _ in scheduleExpectation.fulfill() }
            )
        )
        stopDetailsVM.stopData = .init(stopId: "old id", schedules: .init(objects: .init()))

        await stopDetailsVM.handleStopChange(stop.id)

        await fulfillment(of: [leaveExpectation, joinExpectation, scheduleExpectation], timeout: 1)
        XCTAssertEqual(
            StopData(
                stopId: stop.id,
                schedules: .init(objects: objects),
                predictionsByStop: .init(objects: objects),
                predictionsLoaded: true
            ),
            stopDetailsVM.stopData
        )
    }

    func testLoadPredictions() async throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let predictions = PredictionsByStopJoinResponse(objects: objects)
        let connectExp = expectation(description: "predictions are connected")
        let disconnectExp = expectation(description: "predictions are disconnected")
        let stopDetailsVM = StopDetailsViewModel(
            predictionsRepository: MockPredictionsRepository(
                onConnectV2: { _ in connectExp.fulfill() },
                onDisconnect: { disconnectExp.fulfill() },
                connectV2Response: predictions
            )
        )
        stopDetailsVM.stopData = .init(
            stopId: stop.id,
            schedules: .init(objects: objects),
            predictionsByStop: nil,
            predictionsLoaded: false
        )

        stopDetailsVM.joinStopPredictions(stop.id)
        await fulfillment(of: [connectExp], timeout: 1)
        try await Task.sleep(for: .seconds(1))
        XCTAssertEqual(stopDetailsVM.stopData?.predictionsByStop, predictions)
        stopDetailsVM.leaveStopPredictions()
        await fulfillment(of: [disconnectExp], timeout: 1)
    }

    func testGetRouteCardData() async throws {
        let now = EasternTimeInstant.now()
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
            schedule.departureTime = now.plus(minutes: 10)
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
            schedule.departureTime = now.plus(minutes: 10)
        }
        let upcoming1 = objects.upcomingTrip(
            schedule: schedule1,
            prediction: objects.prediction(schedule: schedule1) { prediction in
                prediction.trip = trip1
                prediction.routeId = route.id
                prediction.stopId = stop.id
                prediction.departureTime = now.plus(minutes: 10)
            }
        )
        let direction1 = Direction(
            name: (route.directionNames[1] as? String)!,
            destination: (route.directionDestinations[1] as? String)!,
            id: 1
        )

        let stopDetailsVM = StopDetailsViewModel()
        stopDetailsVM.global = .init(objects: objects, patternIdsByStop: [stop.id: [pattern0.id, pattern1.id]])
        stopDetailsVM.stopData = .init(
            stopId: stop.id,
            schedules: .init(objects: objects),
            predictionsByStop: .init(objects: objects),
            predictionsLoaded: true
        )

        let routeCardData = await stopDetailsVM.getRouteCardData(
            stopId: stop.id,
            alerts: .init(objects: objects),
            now: now,
            isFiltered: false
        )

        let context = RouteCardData.Context.stopDetailsUnfiltered
        XCTAssertEqual([RouteCardData(
            lineOrRoute: .route(route),
            stopData: [
                .init(lineOrRoute: .route(route), stop: stop, directions: [direction0, direction1], data: [
                    .init(
                        lineOrRoute: .route(route), stop: stop,
                        directionId: 0,
                        routePatterns: [pattern0],
                        stopIds: [stop.id],
                        upcomingTrips: [upcoming0],
                        alertsHere: [],
                        allDataLoaded: true,
                        hasSchedulesToday: true,
                        alertsDownstream: [],
                        context: context
                    ),
                    .init(
                        lineOrRoute: .route(route), stop: stop,
                        directionId: 1,
                        routePatterns: [pattern1],
                        stopIds: [stop.id],
                        upcomingTrips: [upcoming1],
                        alertsHere: [],
                        allDataLoaded: true,
                        hasSchedulesToday: true,
                        alertsDownstream: [],
                        context: context
                    ),
                ]),
            ], at: now
        )], routeCardData)
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
        try await Task.sleep(for: .seconds(1))
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

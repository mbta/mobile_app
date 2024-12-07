//
//  TripDetailsViewTests.swift
//  iosAppTests
//
//  Created by esimon on 12/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import SwiftUI
import ViewInspector
import XCTest

final class TripDetailsViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testDisplaysVehicleCard() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)
        let targetStop = objects.stop { _ in }
        let vehicleStop = objects.stop { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.routeId = route.id
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = vehicleStop.id
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }
        objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(5).toKotlinInstant()
            prediction.vehicleId = vehicle.id
        }

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Outcome: .init(objects: objects)),
            tripPredictionsRepository: MockTripPredictionsRepository(),
            tripRepository: MockTripRepository(
                tripSchedulesResponse: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripResponse: .init(trip: trip)
            ),
            vehicleRepository: MockVehicleRepository(outcome: ApiResultOk(data: .init(vehicle: vehicle)))
        )
        stopDetailsVM.global = .init(objects: objects)
        stopDetailsVM.predictionsByStop = .init(objects: objects)
        stopDetailsVM.pinnedRoutes = .init()
        stopDetailsVM.tripPredictions = .init(objects: objects)
        stopDetailsVM.trip = trip
        stopDetailsVM.tripSchedules = TripSchedulesResponse.Schedules(schedules: [schedule])
        stopDetailsVM.vehicle = vehicle
        stopDetailsVM.tripPredictionsLoaded = true

        let sut = TripDetailsView(
            tripId: trip.id,
            vehicleId: vehicle.id,
            routeId: route.id,
            stopId: targetStop.id,
            stopSequence: vehicle.currentStopSequence?.intValue,
            now: now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM
        )

        XCTAssertNotNil(try sut.inspect().find(TripVehicleCard.self).find(text: "Next stop"))
        XCTAssertNotNil(try sut.inspect().find(TripVehicleCard.self).find(text: vehicleStop.name))
    }

    func testDisplaysStopList() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)
        let targetStop = objects.stop { _ in }
        let vehicleStop = objects.stop { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.routeId = route.id
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = vehicleStop.id
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }
        objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.addingTimeInterval(5).toKotlinInstant()
            prediction.vehicleId = vehicle.id
        }

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Outcome: .init(objects: objects)),
            tripPredictionsRepository: MockTripPredictionsRepository(),
            tripRepository: MockTripRepository(
                tripSchedulesResponse: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripResponse: .init(trip: trip)
            ),
            vehicleRepository: MockVehicleRepository(outcome: ApiResultOk(data: .init(vehicle: vehicle)))
        )
        stopDetailsVM.global = .init(objects: objects)
        stopDetailsVM.predictionsByStop = .init(objects: objects)
        stopDetailsVM.pinnedRoutes = .init()
        stopDetailsVM.tripPredictions = .init(objects: objects)
        stopDetailsVM.trip = trip
        stopDetailsVM.tripSchedules = TripSchedulesResponse.Schedules(schedules: [schedule])
        stopDetailsVM.vehicle = vehicle
        stopDetailsVM.tripPredictionsLoaded = true

        let sut = TripDetailsView(
            tripId: trip.id,
            vehicleId: vehicle.id,
            routeId: route.id,
            stopId: targetStop.id,
            stopSequence: vehicle.currentStopSequence?.intValue,
            now: now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM
        )

        XCTAssertNotNil(try sut.inspect().find(TripStops.self).find(text: targetStop.name))
    }
}

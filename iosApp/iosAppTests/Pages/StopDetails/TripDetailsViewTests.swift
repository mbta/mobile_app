//
//  TripDetailsViewTests.swift
//  iosAppTests
//
//  Created by esimon on 12/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class TripDetailsViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testDisplaysVehicleCard() throws {
        let now = EasternTimeInstant.now()
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
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Response: .init(objects: objects)),
            tripPredictionsRepository: MockTripPredictionsRepository(),
            tripRepository: MockTripRepository(
                tripSchedulesResponse: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripResponse: .init(trip: trip)
            ),
            vehicleRepository: MockVehicleRepository(outcome: ApiResultOk(data: .init(vehicle: vehicle)))
        )
        stopDetailsVM.global = .init(objects: objects)
        stopDetailsVM.stopData = .init(
            stopId: targetStop.id,
            schedules: .init(objects: objects),
            predictionsByStop: .init(objects: objects),
            predictionsLoaded: true
        )
        stopDetailsVM.tripData = TripData(
            tripFilter: .init(tripId: trip.id, vehicleId: vehicle.id, stopSequence: 0, selectionLock: false),
            trip: trip,
            tripSchedules: TripSchedulesResponse.Schedules(schedules: [schedule]),
            tripPredictions: .init(objects: objects),
            tripPredictionsLoaded: true,
            vehicle: vehicle
        )

        var sut = TripDetailsView(
            tripFilter: stopDetailsVM.tripData?.tripFilter,
            stopId: targetStop.id,
            now: now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            onOpenAlertDetails: { _ in }
        )

        let exp = sut.on(\.didLoadData) { view in
            XCTAssertNotNil(try view.find(TripHeaderCard.self).find(text: "Next stop"))
            XCTAssertNotNil(try view.find(TripHeaderCard.self).find(text: vehicleStop.name))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    func testDisplaysScheduleCard() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }

        let firstStop = objects.stop { _ in }
        let targetStop = objects.stop { _ in }
        let trip = objects.trip(routePattern: pattern) { trip in
            trip.stopIds = [firstStop.id, targetStop.id]
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Response: .companion.empty),
            tripPredictionsRepository: MockTripPredictionsRepository(),
            tripRepository: MockTripRepository(
                tripSchedulesResponse: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripResponse: .init(trip: trip)
            )
        )
        stopDetailsVM.global = .init(objects: objects)
        stopDetailsVM.stopData = .init(
            stopId: targetStop.id,
            schedules: .init(objects: objects),
            predictionsByStop: .init(objects: objects),
            predictionsLoaded: true
        )
        stopDetailsVM.tripData = TripData(
            tripFilter: .init(tripId: trip.id, vehicleId: nil, stopSequence: 0, selectionLock: false),
            trip: trip,
            tripSchedules: TripSchedulesResponse.Schedules(schedules: [schedule]),
            tripPredictions: .init(objects: objects),
            tripPredictionsLoaded: true,
            vehicle: nil
        )

        var sut = TripDetailsView(
            tripFilter: stopDetailsVM.tripData?.tripFilter,
            stopId: targetStop.id,
            now: now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            onOpenAlertDetails: { _ in }
        )

        let exp = sut.on(\.didLoadData) { view in
            let card = try view.find(TripHeaderCard.self)
            try debugPrint(card.findAll(ViewType.Text.self).map { try $0.string() })
            XCTAssertNotNil(try view.find(TripHeaderCard.self).find(text: "Scheduled to depart"))
            XCTAssertNotNil(try view.find(TripHeaderCard.self).find(text: targetStop.name))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    func testDisplaysStopList() throws {
        let now = EasternTimeInstant.now()
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
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Response: .init(objects: objects)),
            tripPredictionsRepository: MockTripPredictionsRepository(),
            tripRepository: MockTripRepository(
                tripSchedulesResponse: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripResponse: .init(trip: trip)
            ),
            vehicleRepository: MockVehicleRepository(outcome: ApiResultOk(data: .init(vehicle: vehicle)))
        )
        stopDetailsVM.global = .init(objects: objects)
        stopDetailsVM.stopData = .init(
            stopId: targetStop.id,
            schedules: .init(objects: objects),
            predictionsByStop: .init(objects: objects),
            predictionsLoaded: true
        )
        stopDetailsVM.tripData = TripData(
            tripFilter: .init(
                tripId: trip.id,
                vehicleId: vehicle.id,
                stopSequence: nil,
                selectionLock: false
            ),
            trip: trip,
            tripSchedules: TripSchedulesResponse.Schedules(schedules: [schedule]),
            tripPredictions: .init(objects: objects),
            tripPredictionsLoaded: true,
            vehicle: vehicle
        )

        var sut = TripDetailsView(
            tripFilter: stopDetailsVM.tripData?.tripFilter,
            stopId: targetStop.id,
            now: now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            onOpenAlertDetails: { _ in }
        )

        let exp = sut.on(\.didLoadData) { view in
            XCTAssertNotNil(try view.find(TripStops.self).find(text: targetStop.name))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    func testTappingDownstreamStopAppendsToNavStack() throws {
        let now = EasternTimeInstant.now()
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
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }
        let oldNavEntry: SheetNavigationStackEntry = .stopDetails(stopId: "oldStop", stopFilter: nil, tripFilter: nil)

        let nearbyVM = NearbyViewModel(navigationStack: [oldNavEntry])
        nearbyVM.alerts = .init(objects: objects)

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Response: .init(objects: objects)),
            tripPredictionsRepository: MockTripPredictionsRepository(),
            tripRepository: MockTripRepository(
                tripSchedulesResponse: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripResponse: .init(trip: trip)
            ),
            vehicleRepository: MockVehicleRepository(outcome: ApiResultOk(data: .init(vehicle: vehicle)))
        )
        stopDetailsVM.global = .init(objects: objects)
        stopDetailsVM.stopData = .init(
            stopId: targetStop.id,
            schedules: .init(objects: objects),
            predictionsByStop: .init(objects: objects),
            predictionsLoaded: true
        )
        stopDetailsVM.tripData = TripData(
            tripFilter: .init(
                tripId: trip.id,
                vehicleId: vehicle.id,
                stopSequence: nil,
                selectionLock: false
            ),
            trip: trip,
            tripSchedules: TripSchedulesResponse.Schedules(schedules: [schedule]),
            tripPredictions: .init(objects: objects),
            tripPredictionsLoaded: true,
            vehicle: vehicle
        )

        let sut = TripDetailsView(
            tripFilter: stopDetailsVM.tripData?.tripFilter,
            stopId: targetStop.id,
            now: now,
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            onOpenAlertDetails: { _ in }
        )

        let newNavEntry: SheetNavigationStackEntry = .stopDetails(
            stopId: targetStop.id,
            stopFilter: nil,
            tripFilter: nil
        )

        sut.onTapStop(stop: TripDetailsStopList.Entry(
            stop: targetStop, stopSequence: 0,
            disruption: nil, schedule: nil, prediction: nil,
            vehicle: nil, routes: [], elevatorAlerts: []
        ))
        XCTAssertEqual(nearbyVM.navigationStack, [oldNavEntry, newNavEntry])
    }
}

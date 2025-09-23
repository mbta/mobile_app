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

    @MainActor
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
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        loadKoinMocks(objects: objects)

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        let tripFilter = TripDetailsPageFilter(
            tripId: trip.id,
            vehicleId: vehicle.id,
            routeId: route.id,
            directionId: 0,
            stopId: targetStop.id,
            stopSequence: nil
        )
        let tripDetailsVM = MockTripDetailsViewModel(initialState: .init(
            tripData: .init(
                tripFilter: tripFilter,
                trip: trip,
                tripSchedules: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripPredictions: .init(objects: objects),
                tripPredictionsLoaded: true,
                vehicle: vehicle
            ),
            stopList: .init(trip: trip, stops: [.init(
                stop: targetStop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: prediction,
                predictionStop: targetStop,
                vehicle: vehicle,
                routes: [route]
            )]),
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            alertSummaries: [:],
            now: now,
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: nearbyVM,
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
        )

        let exp = sut.on(\.didLoadData) { view in
            XCTAssertNotNil(try view.find(TripHeaderCard.self).find(text: "Next stop"))
            XCTAssertNotNil(try view.find(TripHeaderCard.self).find(text: vehicleStop.name))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor
    func testDisplaysScheduleCard() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }

        let firstStop = objects.stop { _ in }
        let targetStop = objects.stop { _ in }
        let trip = objects.trip(routePattern: pattern) { trip in
            trip.stopIds = [firstStop.id, targetStop.id]
            trip.routeId = route.id
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }

        loadKoinMocks(objects: objects)

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        let tripFilter = TripDetailsPageFilter(
            tripId: trip.id,
            vehicleId: nil,
            routeId: route.id,
            directionId: 0,
            stopId: targetStop.id,
            stopSequence: nil
        )
        let firstStopEntry = TripDetailsStopList.Entry(
            stop: firstStop,
            stopSequence: 0,
            disruption: nil,
            schedule: nil,
            prediction: nil,
            vehicle: nil,
            routes: [route]
        )
        let tripDetailsVM = MockTripDetailsViewModel(initialState: .init(
            tripData: .init(
                tripFilter: tripFilter,
                trip: trip,
                tripSchedules: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripPredictions: .init(objects: objects),
                tripPredictionsLoaded: true,
                vehicle: nil
            ),
            stopList: .init(trip: trip, stops: [
                firstStopEntry,
                .init(
                    stop: targetStop,
                    stopSequence: 1,
                    disruption: nil,
                    schedule: schedule,
                    prediction: nil,
                    predictionStop: targetStop,
                    vehicle: nil,
                    routes: [route]
                ),
            ], startTerminalEntry: firstStopEntry),
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            alertSummaries: [:],
            now: now,
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: nearbyVM,
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
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

    @MainActor
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

        loadKoinMocks(objects: objects)

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        let tripFilter = TripDetailsPageFilter(
            tripId: trip.id,
            vehicleId: vehicle.id,
            routeId: route.id,
            directionId: 0,
            stopId: targetStop.id,
            stopSequence: nil
        )
        let tripDetailsVM = MockTripDetailsViewModel(initialState: .init(
            tripData: .init(
                tripFilter: tripFilter,
                trip: trip,
                tripSchedules: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripPredictions: .init(objects: objects),
                tripPredictionsLoaded: true,
                vehicle: vehicle
            ),
            stopList: .init(trip: trip, stops: [.init(
                stop: targetStop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: nil,
                predictionStop: targetStop,
                vehicle: nil,
                routes: [route]
            )]),
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            alertSummaries: [:],
            now: now,
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: nearbyVM,
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
        )

        let exp = sut.on(\.didLoadData) { view in
            XCTAssertNotNil(try view.find(TripStops.self).find(text: targetStop.name))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor
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

        loadKoinMocks(objects: objects)

        let oldNavEntry: SheetNavigationStackEntry = .stopDetails(stopId: "oldStop", stopFilter: nil, tripFilter: nil)
        let nearbyVM = NearbyViewModel(navigationStack: [oldNavEntry])
        nearbyVM.alerts = .init(objects: objects)

        let tripFilter = TripDetailsPageFilter(
            tripId: trip.id,
            vehicleId: vehicle.id,
            routeId: route.id,
            directionId: 0,
            stopId: targetStop.id,
            stopSequence: nil
        )
        let tripDetailsVM = MockTripDetailsViewModel(initialState: .init(
            tripData: .init(
                tripFilter: tripFilter,
                trip: trip,
                tripSchedules: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripPredictions: .init(objects: objects),
                tripPredictionsLoaded: true,
                vehicle: vehicle
            ),
            stopList: .init(trip: trip, stops: [.init(
                stop: targetStop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: nil,
                predictionStop: targetStop,
                vehicle: nil,
                routes: [route]
            )]),
            awaitingPredictionsAfterBackground: false
        ))

        let sut = TripDetailsView(
            tripFilter: tripFilter,
            alertSummaries: [:],
            now: now,
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: nearbyVM,
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
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

    @MainActor
    func testDisplaysFollowButton() throws {
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
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        loadKoinMocks(objects: objects)

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        let tripFilter = TripDetailsPageFilter(
            tripId: trip.id,
            vehicleId: vehicle.id,
            routeId: route.id,
            directionId: 0,
            stopId: targetStop.id,
            stopSequence: nil
        )
        let tripDetailsVM = MockTripDetailsViewModel(initialState: .init(
            tripData: .init(
                tripFilter: tripFilter,
                trip: trip,
                tripSchedules: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripPredictions: .init(objects: objects),
                tripPredictionsLoaded: true,
                vehicle: vehicle
            ),
            stopList: .init(trip: trip, stops: [.init(
                stop: targetStop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: prediction,
                predictionStop: targetStop,
                vehicle: vehicle,
                routes: [route]
            )]),
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            alertSummaries: [:],
            now: now,
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: nearbyVM,
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
        )

        let exp = sut.on(\.didLoadData) { view in
            try view.find(TripHeaderCard.self).find(button: "Follow").tap()
            if case let .tripDetails(filter) = nearbyVM.navigationStack.last {
                XCTAssertEqual(tripFilter, filter)
            } else {
                XCTFail("Nav was not updated to trip details")
            }
        }
        ViewHosting.host(view: sut.withFixedSettings([.trackThisTrip: true]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor
    func testDoesntDisplayFollowButtonOnTripDetails() throws {
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
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        loadKoinMocks(objects: objects)

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        let tripFilter = TripDetailsPageFilter(
            tripId: trip.id,
            vehicleId: vehicle.id,
            routeId: route.id,
            directionId: 0,
            stopId: targetStop.id,
            stopSequence: nil
        )
        let tripDetailsVM = MockTripDetailsViewModel(initialState: .init(
            tripData: .init(
                tripFilter: tripFilter,
                trip: trip,
                tripSchedules: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripPredictions: .init(objects: objects),
                tripPredictionsLoaded: true,
                vehicle: vehicle
            ),
            stopList: .init(trip: trip, stops: [.init(
                stop: targetStop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: prediction,
                predictionStop: targetStop,
                vehicle: vehicle,
                routes: [route]
            )]),
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            alertSummaries: [:],
            isTripDetailsPage: true,
            now: now,
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: nearbyVM,
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
        )

        let exp = sut.on(\.didLoadData) { view in
            XCTAssertThrowsError(try view.find(TripHeaderCard.self).find(button: "Follow"))
        }
        ViewHosting.host(view: sut.withFixedSettings([.trackThisTrip: true]))
        wait(for: [exp], timeout: 1)
    }
}

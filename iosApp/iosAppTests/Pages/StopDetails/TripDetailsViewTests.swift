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
    func testDisplaysVehicleCard() {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)
        let targetStop = objects.stop { _ in }
        let vehicleStop = objects.stop { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.routeId = route.id.idText
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = vehicleStop.id
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id.idText
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        loadKoinMocks(objects: objects)

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
            context: .stopDetails,
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            context: .stopDetails,
            now: now,
            routeAccents: .init(route: route),
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
            navManager: .init(),
        )

        let exp = sut.on(\.didLoadData) { view in
            XCTAssertNotNil(try view.find(TripHeaderCard.self).find(text: vehicleStop.name))
            XCTAssertNotNil(try view.find(TripHeaderCard.self).find(text: "Next stop"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor
    func testDisplaysScheduleCard() {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }

        let firstStop = objects.stop { _ in }
        let targetStop = objects.stop { _ in }
        let trip = objects.trip(routePattern: pattern) { trip in
            trip.stopIds = [firstStop.id, targetStop.id]
            trip.routeId = route.id.idText
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id.idText
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }

        loadKoinMocks(objects: objects)

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
            context: .stopDetails,
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            context: .stopDetails,
            now: now,
            routeAccents: .init(route: route),
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
            navManager: .init(),
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
    func testDisplaysStopList() {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)
        let targetStop = objects.stop { _ in }
        let vehicleStop = objects.stop { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.routeId = route.id.idText
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = vehicleStop.id
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id.idText
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }
        objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        loadKoinMocks(objects: objects)

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
            context: .stopDetails,
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            context: .stopDetails,
            now: now,
            routeAccents: .init(route: route),
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
            navManager: .init(),
        )

        let exp = sut.on(\.didLoadData) { view in
            XCTAssertNotNil(try view.find(TripStops.self).find(text: targetStop.name))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor
    func testTappingDownstreamStopAppendsToNavStack() {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)
        let targetStop = objects.stop { _ in }
        let vehicleStop = objects.stop { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.routeId = route.id.idText
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = vehicleStop.id
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id.idText
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }
        objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        loadKoinMocks(objects: objects)

        let oldNavEntry: SheetNavigationStackEntry = .stopDetails(stopId: "oldStop", stopFilter: nil, tripFilter: nil)
        let navManager = NavigationManager(navigationStack: [oldNavEntry])

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
            context: .stopDetails,
            awaitingPredictionsAfterBackground: false
        ))

        let sut = TripDetailsView(
            tripFilter: tripFilter,
            context: .stopDetails,
            now: now,
            routeAccents: .init(route: route),
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
            navManager: navManager,
        )

        let newNavEntry: SheetNavigationStackEntry = .stopDetails(
            stopId: targetStop.id,
            stopFilter: nil,
            tripFilter: nil
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        let exp = sut.inspection.inspect(after: 0.1) { view in
            try view.find(button: targetStop.name).tap()
            XCTAssertEqual(navManager.navigationStack, [oldNavEntry, newNavEntry])
        }

        wait(for: [exp], timeout: 1)
    }

    @MainActor
    func testDisplaysFollowButton() {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)
        let targetStop = objects.stop { _ in }
        let vehicleStop = objects.stop { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.routeId = route.id.idText
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = vehicleStop.id
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id.idText
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        loadKoinMocks(objects: objects)

        let navManager = NavigationManager()

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
            context: .stopDetails,
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            context: .stopDetails,
            now: now,
            routeAccents: .init(route: route),
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
            navManager: navManager,
        )

        let exp = sut.on(\.didLoadData) { view in
            try view.find(TripHeaderCard.self).find(button: "Follow").tap()
            if case let .tripDetails(filter) = navManager.navigationStack.last {
                XCTAssertEqual(tripFilter, filter)
            } else {
                XCTFail("Nav was not updated to trip details")
            }
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor
    func testDoesntDisplayFollowButtonOnTripDetails() {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)
        let targetStop = objects.stop { _ in }
        let vehicleStop = objects.stop { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.routeId = route.id.idText
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = vehicleStop.id
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id.idText
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        loadKoinMocks(objects: objects)

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
            context: .stopDetails,
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            context: .tripDetails,
            now: now,
            routeAccents: .init(route: route),
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
            navManager: .init(),
        )

        let exp = sut.on(\.didLoadData) { view in
            XCTAssertThrowsError(try view.find(TripHeaderCard.self).find(button: "Follow"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor
    func testDisplaysTripCompleteCard() {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)
        let targetStop = objects.stop { _ in }
        let vehicleStop = objects.stop { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = "different trip"
            vehicle.routeId = route.id.idText
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = vehicleStop.id
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id.idText
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        loadKoinMocks(objects: objects)

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
            context: .stopDetails,
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            context: .tripDetails,
            now: now,
            routeAccents: .init(route: route),
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
            navManager: .init(),
        )

        let exp = sut.on(\.didLoadData) { view in
            XCTAssertNotNil(try view.find(text: "Trip complete"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }

    @MainActor
    func testDisplaysTripNotAvailableCard() {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)
        let targetStop = objects.stop { _ in }
        let vehicleStop = objects.stop { _ in }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.routeId = route.id.idText
            vehicle.currentStatus = .inTransitTo
            vehicle.stopId = vehicleStop.id
        }

        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id.idText
            schedule.stopId = targetStop.id
            schedule.trip = trip
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        loadKoinMocks(objects: objects)

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
                trip: nil,
                tripSchedules: nil,
                tripPredictions: nil,
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
            context: .stopDetails,
            awaitingPredictionsAfterBackground: false
        ))

        var sut = TripDetailsView(
            tripFilter: tripFilter,
            context: .tripDetails,
            now: now,
            routeAccents: .init(route: route),
            onOpenAlertDetails: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            tripDetailsVM: tripDetailsVM,
            navManager: .init(),
        )

        let exp = sut.on(\.didLoadData) { view in
            XCTAssertNotNil(try view.find(text: "Trip not available"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 1)
    }
}

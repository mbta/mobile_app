//
//  TripDetailsPageTests.swift
//  iosApp
//
//  Created by esimon on 9/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class TripDetailsPageTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor
    func testDisplaysRouteHeader() throws {
        let now = EasternTimeInstant.now()
        let objects = TestData.clone()
        let stop = objects.getStop(id: "17863")
        let pattern = objects.getRoutePattern(id: "15-1-0")
        let trip = objects.getTrip(id: pattern.representativeTripId)

        let route = objects.getRoute(id: trip.routeId.idText)
        let vehicle = objects.vehicle { vehicle in
            vehicle.currentStatus = .incomingAt
            vehicle.tripId = trip.id
            vehicle.routeId = route.id.idText
            vehicle.stopId = stop.id
        }
        let schedule = objects.schedule { schedule in
            schedule.routeId = route.id.idText
            schedule.stopId = stop.id
            schedule.trip = trip
        }
        let prediction = objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = now.plus(seconds: 5)
            prediction.vehicleId = vehicle.id
        }

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        let filter = TripDetailsPageFilter(
            tripId: trip.id,
            vehicleId: vehicle.id,
            routeId: route.id,
            directionId: 0,
            stopId: stop.id,
            stopSequence: nil,
        )
        let tripDetailsVM = MockTripDetailsViewModel(initialState: .init(
            tripData: .init(
                tripFilter: filter,
                trip: trip,
                tripSchedules: TripSchedulesResponse.Schedules(schedules: [schedule]),
                tripPredictions: .init(objects: objects),
                tripPredictionsLoaded: true,
                vehicle: vehicle
            ),
            stopList: .init(trip: trip, stops: [.init(
                stop: stop,
                stopSequence: 0,
                disruption: nil,
                schedule: schedule,
                prediction: prediction,
                predictionStop: stop,
                vehicle: vehicle,
                routes: [route]
            )]),
            context: .tripDetails,
            awaitingPredictionsAfterBackground: false
        ))

        let tripDetailsPageVM = MockTripDetailsPageViewModel(initialState: .init(
            direction: Direction(name: "Outbound", destination: "Trip Headsign", id: 0),
            alertSummaries: [:],
            trip: nil,
        ))

        loadKoinMocks(objects: objects)

        let sut = TripDetailsPage(
            filter: filter,
            navCallbacks: .companion.empty,
            nearbyVM: nearbyVM,
            tripDetailsPageVM: tripDetailsPageVM,
            tripDetailsVM: tripDetailsVM
        )

        let exp = expectation(description: "page loaded")
        sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(text: "15"))
            XCTAssertNotNil(try view.find(text: "Outbound to"))
            XCTAssertNotNil(try view.find(text: "Trip Headsign"))
            exp.fulfill()
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [exp], timeout: 1)
    }

    @MainActor
    func testClose() throws {
        let nearbyVM = NearbyViewModel()
        let closeExp = expectation(description: "Page closed")
        let sut = TripDetailsPage(
            filter: .init(
                tripId: "",
                vehicleId: nil,
                routeId: Route.Id(""),
                directionId: 0,
                stopId: "",
                stopSequence: nil
            ),
            navCallbacks: .init(onBack: nil, onClose: { closeExp.fulfill() }, backButtonPresentation: .floating),
            nearbyVM: nearbyVM,
        )
        try sut.inspect().find(ActionButton.self).implicitAnyView().button().tap()
        wait(for: [closeExp], timeout: 1)
    }
}

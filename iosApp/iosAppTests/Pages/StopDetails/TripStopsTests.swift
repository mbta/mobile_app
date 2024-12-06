//
//  TripStopsTests.swift
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

final class TripStopsTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testDisplaysSplitStops() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)

        let stop1 = objects.stop { _ in }
        let stop2 = objects.stop { _ in }
        let stop3Target = objects.stop { _ in }
        let stop4 = objects.stop { _ in }
        let stop5 = objects.stop { _ in }

        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.routeId = route.id
            vehicle.currentStatus = .stoppedAt
            vehicle.stopId = stop1.id
        }

        func makeSchedule(stop: Stop) -> Schedule {
            objects.schedule { schedule in
                schedule.routeId = route.id
                schedule.stopId = stop.id
                schedule.trip = trip
            }
        }

        var predictionTime = now
        func makePrediction(schedule: Schedule) -> Prediction {
            predictionTime = predictionTime.addingTimeInterval(5)
            return objects.prediction(schedule: schedule) { prediction in
                prediction.departureTime = predictionTime.toKotlinInstant()
                prediction.vehicleId = vehicle.id
            }
        }

        let schedule1 = makeSchedule(stop: stop1)
        let prediction1 = makePrediction(schedule: schedule1)
        let schedule2 = makeSchedule(stop: stop2)
        let prediction2 = makePrediction(schedule: schedule2)
        let schedule3 = makeSchedule(stop: stop3Target)
        let prediction3 = makePrediction(schedule: schedule3)
        let schedule4 = makeSchedule(stop: stop4)
        let prediction4 = makePrediction(schedule: schedule4)
        let schedule5 = makeSchedule(stop: stop5)
        let prediction5 = makePrediction(schedule: schedule5)

        let stops = TripDetailsStopList(stops: [
            .init(
                stop: stop1, stopSequence: 1, alert: nil,
                schedule: schedule1, prediction: prediction1,
                vehicle: vehicle, routes: [route]
            ),
            .init(
                stop: stop2, stopSequence: 2, alert: nil,
                schedule: schedule2, prediction: prediction2,
                vehicle: vehicle, routes: [route]
            ),
            .init(
                stop: stop3Target, stopSequence: 3, alert: nil,
                schedule: schedule3, prediction: prediction3,
                vehicle: vehicle, routes: [route]
            ),
            .init(
                stop: stop4, stopSequence: 4, alert: nil,
                schedule: schedule4, prediction: prediction4,
                vehicle: vehicle, routes: [route]
            ),
            .init(
                stop: stop5, stopSequence: 5, alert: nil,
                schedule: schedule5, prediction: prediction5,
                vehicle: vehicle, routes: [route]
            ),
        ])

        let sut = TripStops(
            targetId: stop3Target.id,
            stops: stops,
            stopSequence: 1,
            vehicleShown: true,
            now: now,
            onTapLink: { _, _, _ in },
            route: route,
            global: .init(objects: objects)
        )

        XCTAssertNotNil(try sut.inspect().find(text: "2 stops away"))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "stop-pin-indicator"
        }))
        XCTAssertNotNil(try sut.inspect().find(text: stop3Target.name))
        XCTAssertNotNil(try sut.inspect().find(text: stop5.name))
    }

    func testDisplaysUnsplitStops() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)

        let stopTarget = objects.stop { _ in }
        let stop1 = objects.stop { _ in }
        let stop2 = objects.stop { _ in }
        let stop3 = objects.stop { _ in }

        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.routeId = route.id
            vehicle.currentStatus = .stoppedAt
            vehicle.stopId = stop1.id
        }

        func makeSchedule(stop: Stop) -> Schedule {
            objects.schedule { schedule in
                schedule.routeId = route.id
                schedule.stopId = stop.id
                schedule.trip = trip
            }
        }

        var predictionTime = now
        func makePrediction(schedule: Schedule) -> Prediction {
            predictionTime = predictionTime.addingTimeInterval(5)
            return objects.prediction(schedule: schedule) { prediction in
                prediction.departureTime = predictionTime.toKotlinInstant()
                prediction.vehicleId = vehicle.id
            }
        }

        let schedule1 = makeSchedule(stop: stop1)
        let prediction1 = makePrediction(schedule: schedule1)
        let schedule2 = makeSchedule(stop: stop2)
        let prediction2 = makePrediction(schedule: schedule2)
        let schedule3 = makeSchedule(stop: stop3)
        let prediction3 = makePrediction(schedule: schedule3)

        let stops = TripDetailsStopList(stops: [
            .init(
                stop: stop1, stopSequence: 1, alert: nil,
                schedule: schedule1, prediction: prediction1,
                vehicle: vehicle, routes: [route]
            ),
            .init(
                stop: stop2, stopSequence: 2, alert: nil,
                schedule: schedule2, prediction: prediction2,
                vehicle: vehicle, routes: [route]
            ),
            .init(
                stop: stop3, stopSequence: 3, alert: nil,
                schedule: schedule3, prediction: prediction3,
                vehicle: vehicle, routes: [route]
            ),
        ])

        let sut = TripStops(
            targetId: stopTarget.id,
            stops: stops,
            stopSequence: 0,
            vehicleShown: true,
            now: now,
            onTapLink: { _, _, _ in },
            route: route,
            global: .init(objects: objects)
        )

        XCTAssertThrowsError(try sut.inspect().find(ViewType.Image.self, where: { image in
            try image.actualImage().name() == "stop-pin-indicator"
        }))
        XCTAssertNotNil(try sut.inspect().find(text: stop1.name))
        XCTAssertNotNil(try sut.inspect().find(text: stop2.name))
        XCTAssertNotNil(try sut.inspect().find(text: stop3.name))
    }
}

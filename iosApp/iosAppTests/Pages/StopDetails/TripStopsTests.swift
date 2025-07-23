//
//  TripStopsTests.swift
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

        let stop1 = objects.stop { stop in stop.name = "Stop A" }
        let stop2 = objects.stop { stop in stop.name = "Stop B" }
        let stop3Target = objects.stop { stop in stop.name = "Stop C" }
        let stop4 = objects.stop { stop in stop.name = "Stop D" }
        let stop5 = objects.stop { stop in stop.name = "Stop E" }

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

        let stops = TripDetailsStopList(trip: trip, stops: [
            .init(
                stop: stop1, stopSequence: 1, disruption: nil,
                schedule: schedule1, prediction: prediction1,
                vehicle: vehicle, routes: [route], elevatorAlerts: []
            ),
            .init(
                stop: stop2, stopSequence: 2, disruption: nil,
                schedule: schedule2, prediction: prediction2,
                vehicle: vehicle, routes: [route], elevatorAlerts: []
            ),
            .init(
                stop: stop3Target, stopSequence: 3, disruption: nil,
                schedule: schedule3, prediction: prediction3,
                vehicle: vehicle, routes: [route], elevatorAlerts: []
            ),
            .init(
                stop: stop4, stopSequence: 4, disruption: nil,
                schedule: schedule4, prediction: prediction4,
                vehicle: vehicle, routes: [route], elevatorAlerts: []
            ),
            .init(
                stop: stop5, stopSequence: 5, disruption: nil,
                schedule: schedule5, prediction: prediction5,
                vehicle: vehicle, routes: [route], elevatorAlerts: []
            ),
        ])

        let sut = TripStops(
            targetId: stop3Target.id,
            stops: stops,
            stopSequence: 1,
            headerSpec: .vehicle(vehicle, stop1, nil, false),
            now: now,
            alertSummaries: [:],
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            global: .init(objects: objects)
        ).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: "2 stops away"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "stop-pin-indicator"))
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
        let stop1 = objects.stop { stop in stop.name = "Stop A" }
        let stop2 = objects.stop { stop in stop.name = "Stop B" }
        let stop3 = objects.stop { stop in stop.name = "Stop C" }

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

        let stops = TripDetailsStopList(trip: trip, stops: [
            .init(
                stop: stop1, stopSequence: 1, disruption: nil,
                schedule: schedule1, prediction: prediction1,
                vehicle: vehicle, routes: [route], elevatorAlerts: []
            ),
            .init(
                stop: stop2, stopSequence: 2, disruption: nil,
                schedule: schedule2, prediction: prediction2,
                vehicle: vehicle, routes: [route], elevatorAlerts: []
            ),
            .init(
                stop: stop3, stopSequence: 3, disruption: nil,
                schedule: schedule3, prediction: prediction3,
                vehicle: vehicle, routes: [route], elevatorAlerts: []
            ),
        ])

        let sut = TripStops(
            targetId: stopTarget.id,
            stops: stops,
            stopSequence: 0,
            headerSpec: TripHeaderSpec.vehicle(vehicle, stop1, nil, false),
            now: now,
            alertSummaries: [:],
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            global: .init(objects: objects)
        ).withFixedSettings([:])

        XCTAssertThrowsError(try sut.inspect().find(imageName: "stop-pin-indicator"))
        XCTAssertNotNil(try sut.inspect().find(text: stop1.name))
        XCTAssertNotNil(try sut.inspect().find(text: stop2.name))
        XCTAssertNotNil(try sut.inspect().find(text: stop3.name))
    }

    func testTargetHidden() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)

        let stop1 = objects.stop { stop in stop.name = "Stop A" }
        let stop2 = objects.stop { stop in stop.name = "Stop B" }

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

        let firstStop: TripDetailsStopList.Entry = .init(
            stop: stop1, stopSequence: 1, disruption: nil,
            schedule: schedule1, prediction: prediction1,
            vehicle: vehicle, routes: [route], elevatorAlerts: []
        )
        let stops = TripDetailsStopList(
            trip: trip,
            stops: [
                firstStop,
                .init(
                    stop: stop2, stopSequence: 2, disruption: nil,
                    schedule: schedule2, prediction: prediction2,
                    vehicle: vehicle, routes: [route], elevatorAlerts: []
                ),
            ],
            startTerminalEntry: firstStop
        )

        let sut = TripStops(
            targetId: stop1.id,
            stops: stops,
            stopSequence: 1,
            headerSpec: .scheduled(stop1, firstStop),
            now: now,
            alertSummaries: [:],
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            global: .init(objects: objects)
        ).withFixedSettings([:])

        XCTAssertThrowsError(try sut.inspect().find(text: stop1.name))
        XCTAssertThrowsError(try sut.inspect().find(imageName: "stop-pin-indicator"))
    }

    func testFirstStopSeparated() throws {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let route = objects.route { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)

        let stop1 = objects.stop { stop in stop.name = "Stop A" }
        let stop2 = objects.stop { stop in stop.name = "Stop B" }
        let stop3 = objects.stop { stop in stop.name = "Stop C" }

        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = "different"
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

        let firstStop: TripDetailsStopList.Entry = .init(
            stop: stop1, stopSequence: 1, disruption: nil,
            schedule: schedule1, prediction: prediction1,
            vehicle: vehicle, routes: [route], elevatorAlerts: []
        )
        let stops = TripDetailsStopList(
            trip: trip,
            stops: [
                firstStop,
                .init(
                    stop: stop2, stopSequence: 2, disruption: nil,
                    schedule: schedule2, prediction: prediction2,
                    vehicle: vehicle, routes: [route], elevatorAlerts: []
                ),
                .init(
                    stop: stop3, stopSequence: 3, disruption: nil,
                    schedule: schedule3, prediction: prediction3,
                    vehicle: vehicle, routes: [route], elevatorAlerts: []
                ),
            ],
            startTerminalEntry: firstStop
        )

        let sut = TripStops(
            targetId: stop3.id,
            stops: stops,
            stopSequence: 3,
            headerSpec: .finishingAnotherTrip,
            now: now,
            alertSummaries: [:],
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: TripRouteAccents(route: route),
            global: .init(objects: objects)
        ).withFixedSettings([:])

        let firstRow = try sut.inspect().findAll(TripStopRow.self).first!
        XCTAssertNotNil(try firstRow.find(text: stop1.name))
        XCTAssert(try firstRow.actualView().firstStop)
        XCTAssertNotNil(try sut.inspect().find(text: "1 stop away"))
    }

    func testDownstreamShuttleAlert() {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)

        let stop1 = objects.stop { $0.name = "Stop A" }
        let stop2Target = objects.stop { $0.name = "Stop B" }
        let stop3 = objects.stop { $0.name = "Stop C" }

        let alert = objects.alert { $0.effect = .shuttle }

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
            predictionTime += 5
            return objects.prediction(schedule: schedule) { prediction in
                prediction.departureTime = predictionTime.toKotlinInstant()
                prediction.vehicleId = vehicle.id
            }
        }

        let schedule1 = makeSchedule(stop: stop1)
        let prediction1 = makePrediction(schedule: schedule1)
        let schedule2 = makeSchedule(stop: stop2Target)
        let prediction2 = makePrediction(schedule: schedule2)
        let schedule3 = makeSchedule(stop: stop3)
        let prediction3 = makePrediction(schedule: schedule3)

        let stops = TripDetailsStopList(trip: trip, stops: [
            .init(
                stop: stop1,
                stopSequence: 1,
                disruption: nil,
                schedule: schedule1,
                prediction: prediction1,
                vehicle: vehicle,
                routes: [route]
            ),
            .init(
                stop: stop2Target,
                stopSequence: 2,
                disruption: nil,
                schedule: schedule2,
                prediction: prediction2,
                vehicle: vehicle,
                routes: [route]
            ),
            .init(
                stop: stop3,
                stopSequence: 3,
                disruption: .init(alert: alert, mapStopRoute: nil),
                schedule: schedule3,
                prediction: prediction3,
                vehicle: vehicle,
                routes: [route]
            ),
        ])

        let sut = TripStops(
            targetId: stop2Target.id,
            stops: stops,
            stopSequence: 1,
            headerSpec: .vehicle(vehicle, stop1, nil, false),
            now: now,
            alertSummaries: [alert.id: .init(
                effect: alert.effect,
                location: AlertSummary.LocationSingleStop(stopName: stop3.name),
                timeframe: AlertSummary.TimeframeEndOfService.shared
            )],
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: .init(route: route),
            global: .init(objects: objects)
        ).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: "Shuttle buses at Stop C through end of service"))
    }

    func testDownstreamClosureAlert() {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)

        let stop1 = objects.stop { $0.name = "Stop A" }
        let stop2Target = objects.stop { $0.name = "Stop B" }
        let stop3 = objects.stop { $0.name = "Stop C" }

        let alert = objects.alert { $0.effect = .stopClosure }

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
            predictionTime += 5
            return objects.prediction(schedule: schedule) { prediction in
                prediction.departureTime = predictionTime.toKotlinInstant()
                prediction.vehicleId = vehicle.id
            }
        }

        let schedule1 = makeSchedule(stop: stop1)
        let prediction1 = makePrediction(schedule: schedule1)
        let schedule2 = makeSchedule(stop: stop2Target)
        let prediction2 = makePrediction(schedule: schedule2)
        let schedule3 = makeSchedule(stop: stop3)
        let prediction3 = makePrediction(schedule: schedule3)

        let stops = TripDetailsStopList(trip: trip, stops: [
            .init(
                stop: stop1,
                stopSequence: 1,
                disruption: nil,
                schedule: schedule1,
                prediction: prediction1,
                vehicle: vehicle,
                routes: [route]
            ),
            .init(
                stop: stop2Target,
                stopSequence: 2,
                disruption: nil,
                schedule: schedule2,
                prediction: prediction2,
                vehicle: vehicle,
                routes: [route]
            ),
            .init(
                stop: stop3,
                stopSequence: 3,
                disruption: .init(alert: alert, mapStopRoute: nil),
                schedule: schedule3,
                prediction: prediction3,
                vehicle: vehicle,
                routes: [route]
            ),
        ])

        let sut = TripStops(
            targetId: stop2Target.id,
            stops: stops,
            stopSequence: 1,
            headerSpec: .vehicle(vehicle, stop1, nil, false),
            now: now,
            alertSummaries: [alert.id: .init(
                effect: alert.effect,
                location: AlertSummary.LocationSingleStop(stopName: stop3.name),
                timeframe: AlertSummary.TimeframeEndOfService.shared
            )],
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: .init(route: route),
            global: .init(objects: objects)
        ).withFixedSettings([:])

        XCTAssertThrowsError(try sut.inspect().find(text: "Stop closed at Stop C through end of service"))
    }

    func testUpstreamAlert() {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let pattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: pattern)

        let stop1 = objects.stop { $0.name = "Stop A" }
        let stop2Target = objects.stop { $0.name = "Stop B" }
        let stop3 = objects.stop { $0.name = "Stop C" }

        let alert = objects.alert { $0.effect = .stopClosure }

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
            predictionTime += 5
            return objects.prediction(schedule: schedule) { prediction in
                prediction.departureTime = predictionTime.toKotlinInstant()
                prediction.vehicleId = vehicle.id
            }
        }

        let schedule1 = makeSchedule(stop: stop1)
        let prediction1 = makePrediction(schedule: schedule1)
        let schedule2 = makeSchedule(stop: stop2Target)
        let prediction2 = makePrediction(schedule: schedule2)
        let schedule3 = makeSchedule(stop: stop3)
        let prediction3 = makePrediction(schedule: schedule3)

        let stops = TripDetailsStopList(trip: trip, stops: [
            .init(
                stop: stop1,
                stopSequence: 1,
                disruption: .init(alert: alert, mapStopRoute: nil),
                schedule: schedule1,
                prediction: prediction1,
                vehicle: vehicle,
                routes: [route]
            ),
            .init(
                stop: stop2Target,
                stopSequence: 2,
                disruption: nil,
                schedule: schedule2,
                prediction: prediction2,
                vehicle: vehicle,
                routes: [route]
            ),
            .init(
                stop: stop3,
                stopSequence: 3,
                disruption: nil,
                schedule: schedule3,
                prediction: prediction3,
                vehicle: vehicle,
                routes: [route]
            ),
        ])

        let sut = TripStops(
            targetId: stop2Target.id,
            stops: stops,
            stopSequence: 1,
            headerSpec: .vehicle(vehicle, stop1, nil, false),
            now: now,
            alertSummaries: [alert.id: .init(
                effect: alert.effect,
                location: AlertSummary.LocationSingleStop(stopName: stop1.name),
                timeframe: AlertSummary.TimeframeEndOfService.shared
            )],
            onTapLink: { _ in },
            onOpenAlertDetails: { _ in },
            routeAccents: .init(route: route),
            global: .init(objects: objects)
        ).withFixedSettings([:])

        XCTAssertNil(try? sut.inspect().find(textWhere: { text, _ in text.contains("Stop closed") }))
    }
}

//
//  AlertCardTests.swift
//  iosAppTests
//
//  Created by esimon on 1/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class AlertCardTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testMajorAlertCard() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .stationClosure
            alert.header = "Test header"
            alert.activePeriod(
                start: now.minus(hours: 3 * 24),
                end: now.plus(hours: 3 * 24)
            )
        }

        let exp = XCTestExpectation(description: "Detail button pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .major,
            routeAccents: .init(),
            onViewDetails: {
                exp.fulfill()
            }
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Station Closure"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "alert-borderless-suspension"))
        XCTAssertThrowsError(try sut.inspect().find(imageName: "fa-circle-info"))
        try sut.inspect().find(button: "View details").tap()
        wait(for: [exp], timeout: 1)
    }

    func testMajorAlertCardSummaryThroughTomorrow() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .shuttle
            alert.header = "Test header"
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.Standard(effect: .shuttle,
                                                location: .some(AlertSummary.LocationSuccessiveStops(
                                                    startStopName: "Start Stop",
                                                    endStopName: "End Stop"
                                                )),
                                                timeframe: .some(AlertSummary.TimeframeTomorrow()),
                                                recurrence: nil,
                                                isUpdate: false),
            spec: .major,
            routeAccents: .init(),
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Shuttle buses from Start Stop to End Stop through tomorrow"))
    }

    func testMajorAlertCardSummaryThroughEndOfService() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .stopClosure
            alert.header = "Test header"
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.Standard(effect: .stopClosure,
                                                location: .some(AlertSummary
                                                    .LocationSingleStop(stopName: "Single Stop")),
                                                timeframe: .some(AlertSummary.TimeframeEndOfService()),
                                                recurrence: nil,
                                                isUpdate: false),
            spec: .major,
            routeAccents: .init(),
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Stop closed at Single Stop through end of service"))
    }

    func testMajorAlertCardSummaryThroughLaterDate() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .shuttle
            alert.header = "Test header"
        }

        let exp = XCTestExpectation(description: "Detail button pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.Standard(
                effect: .shuttle,
                location: .some(AlertSummary.LocationStopToDirection(
                    startStopName: "Start Stop",
                    direction: Direction(name: "West", destination: "Destination", id: 0)
                )),
                timeframe: .some(AlertSummary.TimeframeLaterDate(
                    time: EasternTimeInstant(year: 2025, month: .april, day: 16, hour: 16, minute: 0, second: 0)
                )),
                recurrence: nil,
                isUpdate: false
            ),
            spec: .major,
            routeAccents: .init(),
            onViewDetails: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Shuttle buses from Start Stop to Westbound stops through Apr 16"))
    }

    func testMajorAlertCardSummaryThroughThisWeek() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .shuttle
            alert.header = "Test header"
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.Standard(
                effect: .shuttle,
                location: .some(AlertSummary.LocationDirectionToStop(
                    direction: Direction(name: "West", destination: "Destination", id: 0),
                    endStopName: "End Stop"
                )),
                timeframe: .some(AlertSummary.TimeframeThisWeek(
                    time: EasternTimeInstant(year: 2025, month: .april, day: 16, hour: 16, minute: 0, second: 0)
                )),
                recurrence: nil,
                isUpdate: false
            ),
            spec: .major,
            routeAccents: .init(),
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect()
            .find(text: "Shuttle buses from Westbound stops to End Stop through Wednesday"))
    }

    func testMajorAlertCardSummaryThroughTime() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .shuttle
            alert.header = "Test header"
        }

        let time = EasternTimeInstant(year: 2025, month: .april, day: 16, hour: 16, minute: 0, second: 0)

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.Standard(
                effect: .shuttle,
                location: .some(AlertSummary.LocationSuccessiveStops(
                    startStopName: "Start Stop",
                    endStopName: "End Stop"
                )),
                timeframe: .some(AlertSummary.TimeframeTime(time: time)),
                recurrence: nil,
                isUpdate: false
            ),
            spec: .major,
            routeAccents: .init(),
            onViewDetails: {}
        )

        XCTAssertNotNil(try sut.inspect()
            .find(text: "Shuttle buses from Start Stop to End Stop through 4:00\u{202F}PM"))
    }

    func testSecondaryAlertCard() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .detour
            alert.activePeriod(
                start: now.minus(hours: 3 * 24),
                end: now.plus(hours: 3 * 24)
            )
        }

        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .secondary,
            routeAccents: .init(),
            onViewDetails: {
                exp.fulfill()
            }
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Detour"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "alert-borderless-issue"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "fa-circle-info"))
        try sut.inspect().implicitAnyView().button().tap()
        wait(for: [exp], timeout: 1)
    }

    func testSecondaryAlertCardSummary() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .detour
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.Standard(
                effect: alert.effect,
                location: nil,
                timeframe: AlertSummary.TimeframeTomorrow(),
                recurrence: nil,
                isUpdate: false
            ),
            spec: .secondary,
            routeAccents: .init(),
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Detour through tomorrow"))
    }

    func testDownstreamAlertCard() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .serviceChange
            alert.activePeriod(
                start: now.minus(hours: 3 * 24),
                end: now.plus(hours: 3 * 24)
            )
        }

        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .downstream,
            routeAccents: .init(),
            onViewDetails: {
                exp.fulfill()
            }
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Service change ahead"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "alert-borderless-issue"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "fa-circle-info"))
        try sut.inspect().implicitAnyView().button().tap()
        wait(for: [exp], timeout: 1)
    }

    func testElevatorAlertCard() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .elevatorClosure
            alert.header = "Elevator header"
            alert.activePeriod(
                start: now.minus(hours: 3 * 24),
                end: now.plus(hours: 3 * 24)
            )
        }

        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .elevator,
            routeAccents: .init(),
            onViewDetails: {
                exp.fulfill()
            }
        )
        XCTAssertNotNil(try sut.inspect().find(text: alert.header!))
        XCTAssertNotNil(try sut.inspect().find(imageName: "accessibility-icon-alert"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "fa-circle-info"))
        try sut.inspect().implicitAnyView().button().tap()
        wait(for: [exp], timeout: 1)
    }

    func testElevatorAlertWithFacilityCard() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let facility = objects.facility { facility in
            facility.type = .elevator
            facility.shortName = "Elevator name"
        }
        let alert = objects.alert { alert in
            alert.effect = .elevatorClosure
            alert.header = "Elevator header"
            alert.informedEntity(activities: [.usingWheelchair], facility: facility.id)
            alert.facilities = [facility.id: facility]
            alert.activePeriod(
                start: now.minus(hours: 3 * 24),
                end: now.plus(hours: 3 * 24)
            )
        }

        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .elevator,
            routeAccents: .init(),
            onViewDetails: {
                exp.fulfill()
            }
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Elevator closure (Elevator name)"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "accessibility-icon-alert"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "fa-circle-info"))
        try sut.inspect().implicitAnyView().button().tap()
        wait(for: [exp], timeout: 1)
    }

    func testDelayAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .delay
            alert.header = "header"
            alert.cause = .heavyRidership
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .delay,
            routeAccents: .init(),
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Delays due to heavy ridership"))

        XCTAssertNotNil(try sut.inspect().find(imageName: "fa-circle-info"))
    }

    func testUpcomingDelayAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .delay
            alert.cause = .accident
            alert.header = "Test header"
        }

        let time = EasternTimeInstant(year: 2025, month: .april, day: 16, hour: 21, minute: 0, second: 0)

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.Standard(
                effect: .delay,
                location: AlertSummary.LocationWholeRoute(routeLabel: "Red Line", routeType: .heavyRail),
                timeframe: AlertSummary.TimeframeStartingLaterToday(time: time),
                recurrence: nil,
                isUpdate: false
            ),
            spec: .delay,
            routeAccents: .init(),
            onViewDetails: {}
        )

        XCTAssertNotNil(try sut.inspect()
            .find(text: "Delay on Red Line starting 9:00\u{202F}PM today"))
    }

    func testDelayAlertCardUnknownCause() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .delay
            alert.header = "header"
            alert.cause = .unknownCause
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .delay,
            routeAccents: .init(),
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Delays"))

        XCTAssertNotNil(try sut.inspect().find(imageName: "fa-circle-info"))
    }

    func testSingleTrackingInfoDelay() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .delay
            alert.header = "header"
            alert.severity = 1
            alert.cause = .singleTracking
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .delay,
            routeAccents: .init(),
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Single Tracking"))

        XCTAssertNotNil(try sut.inspect().find(imageName: "fa-circle-info"))
    }

    func testAllClearAlertCard() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .suspension
            alert.activePeriod(
                start: now.minus(hours: 3 * 24),
                end: now.minus(hours: 1 * 24)
            )
        }
        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.AllClear(
                location: .some(AlertSummary.LocationSuccessiveStops(
                    startStopName: "Start Stop",
                    endStopName: "End Stop"
                )),
            ),
            spec: .major,
            routeAccents: .init(),
            onViewDetails: {
                exp.fulfill()
            }
        )

        XCTAssertNotNil(try sut.inspect()
            .find(text: "All clear: Regular service from Start Stop to End Stop"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "alert-borderless-allclear"))
    }

    func testUpdateAlertCard() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .shuttle
            alert.activePeriod(
                start: now.minus(hours: 3 * 24),
                end: now.plus(hours: 3 * 24)
            )
        }
        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.Standard(
                effect: .shuttle,
                location: .some(AlertSummary.LocationSuccessiveStops(
                    startStopName: "Start Stop",
                    endStopName: "End Stop"
                )),
                timeframe: AlertSummary.TimeframeTomorrow(),
                recurrence: nil,
                isUpdate: true
            ),
            spec: .major,
            routeAccents: .init(),
            onViewDetails: {
                exp.fulfill()
            }
        )

        XCTAssertNotNil(try sut.inspect()
            .find(text: "Update: Shuttle buses from Start Stop to End Stop through tomorrow"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "alert-borderless-shuttle"))
    }

    func testTripCancellationAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .cancellation
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.TripSpecific(
                tripIdentity: AlertSummary.TripSpecificTripFrom(
                    tripTime: .init(year: 2026, month: .march, day: 9, hour: 12, minute: 13, second: 0),
                    stopName: "Ruggles"
                ), effect: .cancellation, cause: .mechanicalIssue
            ),
            spec: .major,
            routeAccents: .init(type: .commuterRail),
            onViewDetails: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Train cancelled"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "mode-cr-slash"))
        XCTAssertNotNil(try sut.inspect()
            .find(text: "12:13\u{202F}PM from Ruggles is cancelled today due to mechanical issue"))
    }

    func testMultipleTripSuspensionAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .suspension
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.TripSpecific(
                tripIdentity: AlertSummary.TripSpecificMultipleTrips.shared, effect: .suspension, cause: .holiday
            ),
            spec: .major,
            routeAccents: .init(type: .commuterRail),
            onViewDetails: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Train suspended"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "alert-borderless-suspension"))
        XCTAssertNotNil(try sut.inspect()
            .find(text: "Multiple trips are suspended today due to holiday"))
    }

    func testTripShuttleAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .shuttle
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.TripShuttle(
                tripTime: .init(year: 2026, month: .march, day: 9, hour: 12, minute: 13, second: 0),
                routeType: .commuterRail, currentStopName: "Ruggles", endStopName: "Forest Hills"
            ),
            spec: .major,
            routeAccents: .init(type: .commuterRail),
            onViewDetails: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Shuttle bus"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "alert-borderless-shuttle"))
        XCTAssertNotNil(try sut.inspect()
            .find(text: "Shuttle buses replace the 12:13\u{202F}PM train today from Ruggles to Forest Hills"))
    }

    func testTripStationBypassAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .stationClosure
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.TripSpecific(
                tripIdentity: AlertSummary.TripSpecificTripTo(
                    tripTime: .init(year: 2026, month: .march, day: 9, hour: 12, minute: 13, second: 0),
                    headsign: "Stoughton"
                ), effect: .stationClosure, effectStops: ["Back Bay", "Ruggles"]
            ),
            spec: .major,
            routeAccents: .init(type: .commuterRail),
            onViewDetails: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Stop skipped"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "alert-borderless-suspension"))
        XCTAssertNotNil(try sut.inspect()
            .find(text: "12:13\u{202F}PM to Stoughton will not stop at Back Bay and Ruggles today"))
    }

    func testTripSpecificReminder() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .cancellation
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.TripSpecific(
                tripIdentity: AlertSummary.TripSpecificTripFrom(
                    tripTime: .init(year: 2026, month: .march, day: 9, hour: 12, minute: 13, second: 0),
                    stopName: "Ruggles"
                ), effect: .cancellation, isToday: false, cause: .mechanicalIssue
            ),
            spec: .major,
            routeAccents: .init(type: .commuterRail),
            onViewDetails: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Train cancelled"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "mode-cr-slash"))
        XCTAssertNotNil(try sut.inspect()
            .find(text: "12:13\u{202F}PM from Ruggles is cancelled tomorrow due to mechanical issue"))
    }

    func testTripShuttleRecurrence() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .shuttle
        }

        let sut = AlertCard(
            alert: alert,
            alertSummary: AlertSummary.TripShuttle(
                tripTime: .init(year: 2026, month: .march, day: 9, hour: 12, minute: 13, second: 0),
                routeType: .commuterRail, currentStopName: "Ruggles", endStopName: "Forest Hills",
                recurrence: AlertSummary.RecurrenceDaily(ending: AlertSummary.TimeframeThisWeek(time: .init(
                    year: 2026,
                    month: .march,
                    day: 12,
                    hour: 9,
                    minute: 6,
                    second: 0
                )))
            ),
            spec: .major,
            routeAccents: .init(type: .commuterRail),
            onViewDetails: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Shuttle bus"))
        XCTAssertNotNil(try sut.inspect().find(imageName: "alert-borderless-shuttle"))
        XCTAssertNotNil(try sut.inspect()
            .find(
                text: """
                Shuttle buses replace the 12:13\u{202F}PM train today from Ruggles to Forest Hills daily until Thursday
                """
            ))
    }
}

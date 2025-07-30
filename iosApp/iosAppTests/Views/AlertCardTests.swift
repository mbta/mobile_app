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
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .stationClosure
            alert.header = "Test header"
        }

        let exp = XCTestExpectation(description: "Detail button pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .major,
            color: Color.pink,
            textColor: Color.orange,
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
            alertSummary: AlertSummary(effect: .shuttle,
                                       location: .some(AlertSummary.LocationSuccessiveStops(
                                           startStopName: "Start Stop",
                                           endStopName: "End Stop"
                                       )),
                                       timeframe: .some(AlertSummary.TimeframeTomorrow())),
            spec: .major,
            color: Color.pink,
            textColor: Color.orange,
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
            alertSummary: AlertSummary(effect: .stopClosure,
                                       location: .some(AlertSummary.LocationSingleStop(stopName: "Single Stop")),
                                       timeframe: .some(AlertSummary.TimeframeEndOfService())),
            spec: .major,
            color: Color.pink,
            textColor: Color.orange,
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
            alertSummary: AlertSummary(
                effect: .shuttle,
                location: .some(AlertSummary.LocationStopToDirection(
                    startStopName: "Start Stop",
                    direction: Direction(name: "West", destination: "Destination", id: 0)
                )),
                timeframe: .some(AlertSummary.TimeframeLaterDate(
                    time: EasternTimeInstant(year: 2025, month: .april, day: 16, hour: 16, minute: 0, second: 0)
                ))
            ),
            spec: .major,
            color: Color.pink,
            textColor: Color.orange,
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
            alertSummary: AlertSummary(
                effect: .shuttle,
                location: .some(AlertSummary.LocationDirectionToStop(
                    direction: Direction(name: "West", destination: "Destination", id: 0),
                    endStopName: "End Stop"
                )),
                timeframe: .some(AlertSummary.TimeframeThisWeek(
                    time: EasternTimeInstant(year: 2025, month: .april, day: 16, hour: 16, minute: 0, second: 0)
                ))
            ),
            spec: .major,
            color: Color.pink,
            textColor: Color.orange,
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
            alertSummary: AlertSummary(
                effect: .shuttle,
                location: .some(AlertSummary.LocationSuccessiveStops(
                    startStopName: "Start Stop",
                    endStopName: "End Stop"
                )),
                timeframe: .some(AlertSummary.TimeframeTime(time: time))
            ),
            spec: .major,
            color: Color.pink,
            textColor: Color.orange,
            onViewDetails: {}
        )

        XCTAssertNotNil(try sut.inspect()
            .find(text: "Shuttle buses from Start Stop to End Stop through 4:00\u{202F}PM"))
    }

    func testSecondaryAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .detour
        }

        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .secondary,
            color: Color.pink,
            textColor: Color.orange,
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
            alertSummary: AlertSummary(
                effect: alert.effect,
                location: nil,
                timeframe: AlertSummary.TimeframeTomorrow()
            ),
            spec: .secondary,
            color: Color.pink,
            textColor: Color.orange,
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Detour through tomorrow"))
    }

    func testDownstreamAlertCard() throws {
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .serviceChange
        }

        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .downstream,
            color: Color.pink,
            textColor: Color.orange,
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
        let objects = ObjectCollectionBuilder()
        let alert = objects.alert { alert in
            alert.effect = .elevatorClosure
            alert.header = "Elevator header"
        }

        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .elevator,
            color: Color.pink,
            textColor: Color.orange,
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
        }

        let exp = XCTestExpectation(description: "Card pressed")
        let sut = AlertCard(
            alert: alert,
            alertSummary: nil,
            spec: .elevator,
            color: Color.pink,
            textColor: Color.orange,
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
            color: Color.pink,
            textColor: Color.orange,
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Delays due to heavy ridership"))

        XCTAssertNotNil(try sut.inspect().find(imageName: "fa-circle-info"))
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
            color: Color.pink,
            textColor: Color.orange,
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
            color: Color.pink,
            textColor: Color.orange,
            onViewDetails: {}
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Single Tracking"))

        XCTAssertNotNil(try sut.inspect().find(imageName: "fa-circle-info"))
    }
}

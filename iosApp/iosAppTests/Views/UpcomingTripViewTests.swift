//
//  UpcomingTripViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 5/22/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class UpcomingTripViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
    }

    func testFirstBoardingAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Boarding()),
            routeType: .heavyRail,
            isFirst: true
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "train boarding now"))
    }

    func testBoardingAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Boarding()),
            routeType: .heavyRail,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and boarding now"))
    }

    func testFirstArrivingAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Arriving()),
            routeType: .heavyRail,
            isFirst: true
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "train arriving now"))
    }

    func testArrivingAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Arriving()),
            routeType: .heavyRail,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and arriving now"))
    }

    func testFirstPredictionTimeAccessibilityLabel() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.Time(predictionTime: date, headline: true)),
            routeType: .commuterRail,
            isFirst: true
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/train arriving at 4:00\sPM/#))
    }

    func testPredictionTimeAccessibilityLabel() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.Time(predictionTime: date, headline: true)),
            routeType: .commuterRail,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/and at 4:00\sPM/#))
    }

    func testTimeWithStatus() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.TimeWithStatus(
                predictionTime: date,
                status: "All aboard",
                headline: true
            )),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(
            viewWithAccessibilityLabelMatching: #/train arriving at 4:00\sPM, All aboard/#
        ))
        XCTAssertNotNil(try sut.inspect().find(text: "All aboard"))
    }

    func testScheduleWithStatusColumn() throws {
        let date = EasternTimeInstant(year: 2025, month: .august, day: 5, hour: 14, minute: 26, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.ScheduleTimeWithStatusColumn(scheduledTime: date, status: "Delayed", headline: true)),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/2:26\sPM train delayed/#))
        XCTAssertNotNil(try sut.inspect().find(text: "Delayed"))
    }

    func testScheduleWithStatusRow() throws {
        let date = EasternTimeInstant(year: 2025, month: .august, day: 5, hour: 14, minute: 26, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.ScheduleTimeWithStatusRow(scheduledTime: date, status: "Anomalous")),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect()
            .find(viewWithAccessibilityLabelMatching: #/train arriving at 2:26\sPM scheduled, Anomalous/#))
        XCTAssertNotNil(try sut.inspect().find(text: "Anomalous"))
    }

    func testTimeWithStatusLate() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.TimeWithStatus(
                predictionTime: date,
                status: "Late",
                headline: true
            )),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/4:00\sPM train delayed/#))
    }

    func testTimeWithStatusDelayed() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.TimeWithStatus(
                predictionTime: date,
                status: "Delay",
                headline: true
            )),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/4:00\sPM train delayed/#))
    }

    func testTimeWithScheduleEarly() throws {
        let scheduleDate = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let predictionDate = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 15, minute: 55, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.TimeWithSchedule(
                predictionTime: predictionDate,
                scheduledTime: scheduleDate,
                headline: true
            )),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(
            viewWithAccessibilityLabelMatching: #/4:00\sPM train early, arriving at 3:55\sPM/#
        ))
    }

    func testTimeWithScheduleDelayed() throws {
        let scheduleDate = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let predictionDate = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 5, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.TimeWithSchedule(
                predictionTime: predictionDate,
                scheduledTime: scheduleDate,
                headline: true
            )),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(
            viewWithAccessibilityLabelMatching: #/4:00\sPM train delayed, arriving at 4:05\sPM/#
        ))
    }

    func testFirstScheduledAccessibilityLabel() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.ScheduleTime(scheduledTime: date, headline: true)),
            routeType: .bus,
            isFirst: true,
            isOnly: false
        )
        XCTAssertNotNil(try sut.inspect()
            .find(viewWithAccessibilityLabelMatching: #/buses arriving at 4:00\sPM scheduled/#))
    }

    func testScheduledAccessibilityLabel() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.ScheduleTime(scheduledTime: date, headline: true)),
            routeType: .bus,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/and at 4:00\sPM scheduled/#))
    }

    func testFirstApproachingAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Approaching()),
            routeType: .heavyRail,
            isFirst: true,
            isOnly: false
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "trains arriving in 1 min"))
    }

    func testApproachingAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(.Approaching()), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and in 1 min"))
    }

    func testFirstPredictedAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Minutes(minutes: 5)),
            routeType: .heavyRail,
            isFirst: true,
            isOnly: false
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "trains arriving in 5 min"))
    }

    func testPredictedAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(.Minutes(minutes: 5)), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and in 5 min"))
    }

    func testPredictedHourAccessibilityLabel() throws {
        let sut = UpcomingTripView(prediction: .some(.Minutes(minutes: 67)), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and in 1 hr 7 min"))
    }

    func testCancelledAccessibilityLabel() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(prediction: .some(.Cancelled(scheduledTime: date)),
                                   routeType: .heavyRail,
                                   isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/and at 4:00\sPM cancelled/#))
    }

    func testShuttleAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .disruption(
                .init(alert: ObjectCollectionBuilder.Single.shared.alert { $0.effect = .shuttle }),
                iconName: "alert-borderless-shuttle"
            ),
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Shuttle Bus"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "Shuttle buses replace service"))
    }

    func testSuspensionAccessibilityLabel() throws {
        let sut = UpcomingTripView(
            prediction: .disruption(
                .init(alert: ObjectCollectionBuilder.Single.shared.alert { $0.effect = .suspension }),
                iconName: "alert-borderless-suspension"
            ),
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Suspension"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "Service suspended"))
    }

    func testDetourLabel() throws {
        let sut = UpcomingTripView(
            prediction: .disruption(
                .init(alert: ObjectCollectionBuilder.Single.shared.alert { $0.effect = .detour }),
                iconName: "alert-large-red-issue"
            ),
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Detour"))
    }

    func testDisruptionIconName() throws {
        let alert = ObjectCollectionBuilder.Single.shared.alert { $0.effect = .snowRoute }
        let disruption = UpcomingFormat.Disruption(alert: alert, mapStopRoute: .bus)
        let sut = UpcomingTripView(prediction: .disruption(.init(alert: alert), iconName: disruption.iconName))
        XCTAssertNotNil(try sut.inspect().find(imageName: "alert-large-bus-issue"))
    }
}

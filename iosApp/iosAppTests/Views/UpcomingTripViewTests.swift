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

    func testFirstBoarding() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Boarding(last: false)),
            routeType: .heavyRail,
            isFirst: true
        )
        XCTAssertNotNil(try sut.inspect().find(text: "BRD"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "train boarding now"))
    }

    func testFirstBoardingLast() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Boarding(last: true)),
            routeType: .heavyRail,
            isFirst: true
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect().find(text: "BRD"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "train boarding now, Last trip"))
    }

    func testBoarding() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Boarding(last: false)),
            routeType: .heavyRail,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "BRD"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and boarding now"))
    }

    func testBoardingLast() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Boarding(last: true)),
            routeType: .heavyRail,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect().find(text: "BRD"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and boarding now, Last trip"))
    }

    func testFirstArriving() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Arriving(last: false)),
            routeType: .heavyRail,
            isFirst: true
        )
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "train arriving now"))
    }

    func testFirstArrivingLast() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Arriving(last: true)),
            routeType: .heavyRail,
            isFirst: true
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "train arriving now, Last trip"))
    }

    func testArriving() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Arriving(last: false)),
            routeType: .heavyRail,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and arriving now"))
    }

    func testArrivingLast() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Arriving(last: true)),
            routeType: .heavyRail,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect().find(text: "ARR"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and arriving now, Last trip"))
    }

    func testFirstPredictionTime() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.Time(predictionTime: date, last: false, headline: true)),
            routeType: .commuterRail,
            isFirst: true
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/train arriving at 4:00\sPM/#))
    }

    func testFirstPredictionTimeLast() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.Time(predictionTime: date, last: true, headline: true)),
            routeType: .commuterRail,
            isFirst: true
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect()
            .find(viewWithAccessibilityLabelMatching: #/train arriving at 4:00\sPM, Last trip/#))
    }

    func testPredictionTime() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.Time(predictionTime: date, last: false, headline: true)),
            routeType: .commuterRail,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/and at 4:00\sPM/#))
    }

    func testPredictionTimeLast() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.Time(predictionTime: date, last: true, headline: true)),
            routeType: .commuterRail,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/and at 4:00\sPM, Last trip/#))
    }

    func testTimeWithStatus() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.TimeWithStatus(
                predictionTime: date,
                status: "All aboard",
                last: false,
                headline: true
            )),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(
            viewWithAccessibilityLabelMatching: #/train arriving at 4:00\sPM, All aboard/#
        ))
        XCTAssertNotNil(try sut.inspect().find(text: "All aboard"))
    }

    func testTimeWithStatusLast() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.TimeWithStatus(
                predictionTime: date,
                status: "All aboard",
                last: true,
                headline: true
            )),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect().find(
            viewWithAccessibilityLabelMatching: #/train arriving at 4:00\sPM, All aboard, Last trip/#
        ))
    }

    func testScheduleWithStatusColumn() throws {
        let date = EasternTimeInstant(year: 2025, month: .august, day: 5, hour: 14, minute: 26, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.ScheduleTimeWithStatusColumn(
                scheduledTime: date,
                status: "Delayed",
                last: false, headline: true,
            )),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/2:26\sPM train delayed/#))
        XCTAssertNotNil(try sut.inspect().find(text: "Delayed"))
    }

    func testScheduleWithStatusColumnLast() throws {
        let date = EasternTimeInstant(year: 2025, month: .august, day: 5, hour: 14, minute: 26, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.ScheduleTimeWithStatusColumn(
                scheduledTime: date,
                status: "Delayed",
                last: true, headline: true,
            )),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect()
            .find(viewWithAccessibilityLabelMatching: #/2:26\sPM train delayed, Last trip/#))
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
                last: false,
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
                last: false,
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
                last: false,
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
                last: false,
                headline: true
            )),
            routeType: .commuterRail
        )
        XCTAssertNotNil(try sut.inspect().find(
            viewWithAccessibilityLabelMatching: #/4:00\sPM train delayed, arriving at 4:05\sPM/#
        ))
    }

    func testFirstScheduled() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.ScheduleTime(scheduledTime: date, last: false, headline: true)),
            routeType: .bus,
            isFirst: true,
            isOnly: false
        )
        XCTAssertNotNil(try sut.inspect()
            .find(viewWithAccessibilityLabelMatching: #/buses arriving at 4:00\sPM scheduled/#))
    }

    func testFirstScheduledLast() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.ScheduleTime(scheduledTime: date, last: true, headline: true)),
            routeType: .bus,
            isFirst: true,
            isOnly: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect()
            .find(viewWithAccessibilityLabelMatching: #/buses arriving at 4:00\sPM scheduled, Last trip/#))
    }

    func testScheduled() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.ScheduleTime(scheduledTime: date, last: false, headline: true)),
            routeType: .bus,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(
            viewWithAccessibilityLabelMatching: #/and at 4:00\sPM scheduled/#
        ))
    }

    func testScheduledLast() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(
            prediction: .some(.ScheduleTime(scheduledTime: date, last: true, headline: true)),
            routeType: .bus,
            isFirst: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect().find(
            viewWithAccessibilityLabelMatching: #/and at 4:00\sPM scheduled, Last trip/#
        ))
    }

    func testFirstApproaching() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Approaching(last: false)),
            routeType: .heavyRail,
            isFirst: true,
            isOnly: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "1 min"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "trains arriving in 1 min"))
    }

    func testFirstApproachingLast() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Approaching(last: true)),
            routeType: .heavyRail,
            isFirst: true,
            isOnly: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect().find(text: "1 min"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "trains arriving in 1 min, Last trip"))
    }

    func testApproaching() throws {
        let sut = UpcomingTripView(prediction: .some(.Approaching(last: false)), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(text: "1 min"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and in 1 min"))
    }

    func testApproachingLast() throws {
        let sut = UpcomingTripView(prediction: .some(.Approaching(last: true)), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect().find(text: "1 min"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and in 1 min, Last trip"))
    }

    func testFirstPredicted() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Minutes(minutes: 5, last: false)),
            routeType: .heavyRail,
            isFirst: true,
            isOnly: false
        )
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "trains arriving in 5 min"))
    }

    func testFirstPredictedLast() throws {
        let sut = UpcomingTripView(
            prediction: .some(.Minutes(minutes: 5, last: true)),
            routeType: .heavyRail,
            isFirst: true,
            isOnly: false
        )
        XCTAssertNotNil(try sut.inspect().find(text: "Last"))
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "trains arriving in 5 min, Last trip"))
    }

    func testPredicted() throws {
        let sut = UpcomingTripView(prediction: .some(.Minutes(minutes: 5, last: false)), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and in 5 min"))
    }

    func testPredictedLast() throws {
        let sut = UpcomingTripView(prediction: .some(.Minutes(minutes: 5, last: true)), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and in 5 min, Last trip"))
    }

    func testPredictedHour() throws {
        let sut = UpcomingTripView(prediction: .some(.Minutes(minutes: 67, last: false)), isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabel: "and in 1 hr 7 min"))
    }

    func testCancelled() throws {
        let date = EasternTimeInstant(year: 2024, month: .may, day: 1, hour: 16, minute: 0, second: 0)
        let sut = UpcomingTripView(prediction: .some(.Cancelled(scheduledTime: date)),
                                   routeType: .heavyRail,
                                   isFirst: false)
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityLabelMatching: #/and at 4:00\sPM cancelled/#))
    }

    func testSubwayEarlyMorning() throws {
        let date = EasternTimeInstant(year: 2025, month: .november, day: 17, hour: 10, minute: 22, second: 0)
        let sut =
            UpcomingTripView(prediction: .noTrips(UpcomingFormat.NoTripsFormatSubwayEarlyMorning(scheduledTime: date)))
        XCTAssertNotNil(try sut.inspect().find(textWhere: { value, _ in
            try #/First 10:22\sAM/#.wholeMatch(in: value) != nil
        }))
    }

    func testShuttle() throws {
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

    func testSuspension() throws {
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

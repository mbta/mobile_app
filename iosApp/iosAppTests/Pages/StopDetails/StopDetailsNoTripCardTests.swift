//
//  StopDetailsNoTripCardTests.swift
//  iosAppTests
//
//  Created by esimon on 1/3/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsNoTripCardTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testSubwayEarlyMorning() throws {
        let serviceStart = EasternTimeInstant(year: 2025, month: .november, day: 17, hour: 10, minute: 25, second: 0)
        let sut = StopDetailsNoTripCard(
            status: UpcomingFormat.NoTripsFormatSubwayEarlyMorning(scheduledTime: serviceStart),
            accentColor: Color.text,
            directionLabel: "Forest Hills",
            routeType: .heavyRail,
            now: .now()
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(imageName: "sunrise"))
        XCTAssertNotNil(try sut.inspect().find(text: "Good morning!"))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Divider.self))
        XCTAssertNotNil(try sut.inspect().find(
            textWhere: { value, _ in
                try #/The first Forest Hills train is scheduled to arrive at 10:25\sAM. We don’t have predictions to show you yet, but they’ll appear here closer to the scheduled time./#
                    .wholeMatch(in: value) != nil
            }
        ))
    }

    func testPredictionsUnavailable() throws {
        let sut = StopDetailsNoTripCard(
            status: UpcomingFormat.NoTripsFormatPredictionsUnavailable(),
            accentColor: Color.text,
            directionLabel: "Forest Hills",
            routeType: .bus,
            now: .now()
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(imageName: "live-data-slash"))
        XCTAssertNotNil(try sut.inspect().find(text: "Predictions unavailable"))
        XCTAssertNotNil(try sut.inspect().find(ViewType.Divider.self))
        XCTAssertNotNil(try sut.inspect().find(
            text: "Service is running, but predicted arrival times aren’t available." +
                " Check the map to see where buses are right now."
        ))
    }

    func testServiceEnded() throws {
        let objects = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
        let schedule = objects.schedule { $0.departureTime = .init(
            date: now.local.date.plus(days: 1),
            time: .init(hour: 9, minute: 15, second: 0, nanosecond: 0)
        ) }
        let sut = StopDetailsNoTripCard(
            status: UpcomingFormat.NoTripsFormatServiceEndedToday(),
            accentColor: Color.text,
            directionLabel: "Winthrop",
            routeType: .ferry,
            now: now,
            nextScheduleResponse: .init(nextSchedule: schedule)
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(imageName: "mode-ferry-slash"))
        XCTAssertNotNil(try sut.inspect().find(text: "Service ended"))
        XCTAssertNotNil(try sut.inspect().find(textWhere: { text, _ in
            text.wholeMatch(of: #/Next trip at 9:15\sAM tomorrow/#) != nil
        }))
    }

    func testNoSchedulesToday() throws {
        let objects = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
        let schedule = objects.schedule { $0.departureTime = .init(
            date: now.local.date,
            time: .init(hour: 9, minute: 15, second: 0, nanosecond: 0)
        ) }
        let sut = StopDetailsNoTripCard(
            status: UpcomingFormat.NoTripsFormatNoSchedulesToday(),
            accentColor: Color.text,
            directionLabel: "Fitchburg",
            routeType: .commuterRail,
            now: now,
            nextScheduleResponse: .init(nextSchedule: schedule)
        ).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().find(imageName: "mode-cr-slash"))
        XCTAssertNotNil(try sut.inspect().find(text: "No service today"))
        XCTAssertNotNil(try sut.inspect().find(textWhere: { text, _ in
            text.wholeMatch(of: #/Next trip at 9:15\sAM/#) != nil
        }))
    }
}

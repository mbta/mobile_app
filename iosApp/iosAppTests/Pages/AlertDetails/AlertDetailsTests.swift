//
//  AlertDetailsTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 8/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class AlertDetailsTests: XCTestCase {
    func testBasicAlertDetails() throws {
        let objects = ObjectCollectionBuilder()

        let stop1 = objects.stop { stop in stop.name = "Stop 1" }
        let stop2 = objects.stop { stop in stop.name = "Stop 2" }
        let stop3 = objects.stop { stop in stop.name = "Stop 3" }

        let route = objects.route { route in
            route.type = .heavyRail
            route.longName = "Red Line"
        }

        let now = EasternTimeInstant(year: 2025, month: .july, day: 28, hour: 15, minute: 32, second: 0)

        let alert = objects.alert { alert in
            alert.activePeriod(start: now.minus(seconds: 5), end: now.plus(seconds: 5))
            alert.description_ = "Long description"
            alert.cause = .unrulyPassenger
            alert.effect = .stopClosure
            alert.effectName = "Closure"
            alert.header = "Alert header"
            alert.updatedAt = .init(year: 2025, month: .july, day: 28, hour: 15, minute: 30, second: 0)
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: [route], affectedStops: [stop1, stop2, stop3], now: now)

        XCTAssertNotNil(try sut.inspect().find(text: "Red Line Stop Closure"))
        XCTAssertNotNil(try sut.inspect().find(text: "Unruly Passenger"))
        XCTAssertNotNil(try sut.inspect().find(text: String(alert.activePeriod[0].formatStart().characters)))
        XCTAssertNotNil(try sut.inspect().find(text: "3 affected stops"))
        XCTAssertNotNil(try sut.inspect().find(ViewType.DisclosureGroup.self))
        XCTAssertNotNil(try sut.inspect().find(text: stop1.name))
        XCTAssertNotNil(try sut.inspect().find(text: stop2.name))
        XCTAssertNotNil(try sut.inspect().find(text: stop3.name))
        XCTAssertNotNil(try sut.inspect().find(text: "Full Description"))
        XCTAssertNotNil(try sut.inspect().find(text: alert.description_!))
        XCTAssertNotNil(try sut.inspect().find(text: alert.header!))
        XCTAssertNotNil(try sut.inspect().find(text: "Updated: 7/28/2025, 3:30\u{202F}PM"))
    }

    func testCurrentActivePeriod() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant(year: 2025, month: .july, day: 29, hour: 11, minute: 27, second: 0)

        let alert = objects.alert { alert in
            alert.activePeriod(start: now.minus(minutes: 20), end: now.minus(minutes: 10))
            alert.activePeriod(start: now.minus(minutes: 5), end: now.plus(minutes: 5))
            alert.activePeriod(start: now.plus(minutes: 10), end: now.plus(minutes: 20))
            alert.updatedAt = now.minus(minutes: 100)
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, affectedStops: [], now: now)

        XCTAssertNotNil(try sut.inspect().find(text: "Tuesday, Jul 29, 11:22\u{202F}AM"))
    }

    func testServiceStartAndEndActivePeriod() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant(year: 2025, month: .july, day: 29, hour: 10, minute: 37, second: 0)

        let serviceStart = EasternTimeInstant(year: 2025, month: .july, day: 29, hour: 3, minute: 0, second: 0)
        let serviceEnd = EasternTimeInstant(year: 2025, month: .july, day: 30, hour: 2, minute: 59, second: 0)

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: serviceStart,
                end: serviceEnd
            )
            alert.updatedAt = now.minus(seconds: 100)
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, affectedStops: [], now: now)

        try XCTAssertNotNil(sut.inspect().find(text: "Tuesday, Jul 29, start of service"))
        try XCTAssertThrowsError(sut.inspect().find(textWhere: { string, _ in string.contains("3:00\u{202F}AM") }))
        try XCTAssertNotNil(sut.inspect().find(text: "Tuesday, Jul 29, end of service"))
        try XCTAssertThrowsError(sut.inspect().find(textWhere: { string, _ in string.contains("2:59\u{202F}AM") }))
    }

    func testLaterTodayActivePeriod() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant(year: 2025, month: .july, day: 29, hour: 11, minute: 39, second: 0)

        let start = now.minus(minutes: 20)
        let end = now.plus(hours: 2).plus(minutes: 10)

        let alert = objects.alert { alert in
            alert.durationCertainty = .estimated
            alert.activePeriod(start: start, end: end)
            alert.updatedAt = now.minus(seconds: 100)
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, affectedStops: [], now: now)

        try XCTAssertNotNil(sut.inspect().find(text: "Tuesday, Jul 29, 11:19\u{202F}AM"))
        try XCTAssertThrowsError(sut.inspect().find(textWhere: { string, _ in string.contains("1:39\u{202F}PM") }))
        try XCTAssertNotNil(sut.inspect().find(text: "Tuesday, Jul 29, later today"))
    }

    func testNoCurrentActivePeriod() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant.now()

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.minus(seconds: 10),
                end: now.minus(seconds: 5)
            )
            alert.cause = .unrulyPassenger
            alert.effect = .stopClosure
            alert.effectName = "Closure"
            alert.updatedAt = now.minus(seconds: 100)
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, affectedStops: [], now: now)

        XCTAssertNotNil(try? sut.inspect().find(text: "Alert is no longer in effect"))
        XCTAssertNil(try? sut.inspect().find(text: "Start"))
    }

    func testNoDescription() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant.now()

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.minus(seconds: 5),
                end: now.plus(seconds: 5)
            )
            alert.cause = .unrulyPassenger
            alert.effect = .stopClosure
            alert.effectName = "Closure"
            alert.updatedAt = now.minus(seconds: 100)
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, affectedStops: [], now: now)

        XCTAssertNil(try? sut.inspect().find(text: "Full Description"))
    }

    func testStopsInDescription() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant.now()

        let stop1 = objects.stop { stop in stop.name = "Stop 1" }
        let stop2 = objects.stop { stop in stop.name = "Stop 2" }
        let stop3 = objects.stop { stop in stop.name = "Stop 3" }

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.minus(seconds: 5),
                end: now.plus(seconds: 5)
            )
            alert.cause = .unrulyPassenger
            alert.effect = .stopClosure
            alert.effectName = "Closure"
            alert.updatedAt = now.minus(seconds: 100)
            alert.header = "Alert header"
            alert.description_ = "Alert description\n\nAffected stops:\nStop 1\nStop 2\nStop 3\n\nMore details"
        }

        let sutWithoutStops = AlertDetails(
            alert: alert, line: nil, routes: nil,
            affectedStops: [], now: now
        )

        try print(sutWithoutStops.inspect().findAll(ViewType.Text.self).map { text in try text.string() })
        XCTAssertNil(try? sutWithoutStops.inspect().find(text: "3 affected stops"))
        XCTAssertNotNil(try? sutWithoutStops.inspect().find(text: "Alert description"))
        XCTAssertNotNil(try? sutWithoutStops.inspect().find(text: "Affected stops:\nStop 1\nStop 2\nStop 3"))
        XCTAssertNotNil(try? sutWithoutStops.inspect().find(text: "More details"))

        let sutWithStops = AlertDetails(
            alert: alert, line: nil, routes: nil,
            affectedStops: [stop1, stop2, stop3], now: now
        )

        XCTAssertNotNil(try? sutWithStops.inspect().find(text: "Alert description"))
        XCTAssertNotNil(try? sutWithStops.inspect().find(text: "3 affected stops"))
        XCTAssertNil(try? sutWithStops.inspect().find(text: "Affected stops:\nStop 1\nStop 2\nStop 3"))
        XCTAssertNotNil(try? sutWithStops.inspect().find(text: "More details"))
    }

    func testStopEffectHeader() throws {
        let objects = ObjectCollectionBuilder()

        let stop = objects.stop { stop in stop.name = "Stop" }

        let now = EasternTimeInstant.now()

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.minus(seconds: 5),
                end: now.plus(seconds: 5)
            )
            alert.cause = .maintenance
            alert.effect = .elevatorClosure
            alert.updatedAt = now.minus(seconds: 100)
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, stop: stop, affectedStops: [stop], now: now)

        XCTAssertNotNil(try sut.inspect().find(text: "Stop Elevator Closure"))
        XCTAssertNotNil(try sut.inspect().find(text: "Maintenance"))
    }

    func testEffectOnlyHeader() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant.now()

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.minus(seconds: 5),
                end: now.plus(seconds: 5)
            )
            alert.cause = .freightTrainInterference
            alert.effect = .serviceChange
            alert.updatedAt = now.minus(seconds: 100)
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, stop: nil, affectedStops: [], now: now)

        XCTAssertNotNil(try sut.inspect().find(text: "Service Change"))
    }

    func testElevatorClosure() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant.now()

        let route = objects.route { _ in }
        let stop = objects.stop { $0.name = "Park Street" }
        let alert = objects.alert { alert in
            alert.effect = .elevatorClosure
            alert.header = "Elevator 123 (Foo to Bar) unavailable due to demonstration"
            alert.cause = .demonstration
            alert.activePeriod(
                start: now.minus(hours: 3 * 24),
                end: nil
            )
            alert.informedEntity(
                activities: [.usingWheelchair],
                directionId: nil,
                facility: nil,
                route: nil,
                routeType: nil,
                stop: stop.id,
                trip: nil
            )
            alert.description_ = "To exit, go somewhere."
            alert.updatedAt = now.minus(minutes: 10)
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: [route], stop: stop, affectedStops: [stop], now: now)

        XCTAssertNotNil(try sut.inspect().find(text: "Park Street Elevator Closure"))
        try sut.inspect().findAll(ViewType.Text.self).forEach { try debugPrint($0.string()) }
        XCTAssertNotNil(try sut.inspect().find(text: "Elevator 123 (Foo to Bar) unavailable due to demonstration"))
        XCTAssertNotNil(try sut.inspect().find(text: "Alternative path"))
        XCTAssertNotNil(try sut.inspect().find(text: "To exit, go somewhere."))

        XCTAssertThrowsError(try sut.inspect().find(text: "Demonstration"))
        XCTAssertThrowsError(try sut.inspect().find(text: "Full Description"))
        XCTAssertThrowsError(try sut.inspect().find(text: "1 stop affected"))
    }

    func testRecurringDaily() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant(year: 2026, month: .january, day: 23, hour: 10, minute: 41, second: 0)

        let route = objects.route { _ in }
        let stop = objects.stop { $0.name = "Park Street" }
        let alert = objects.alert { alert in
            alert.effect = .suspension
            alert.activePeriod(
                start: now.minus(hours: 1),
                end: now.plus(hours: 1)
            )
            alert.activePeriod(
                start: now.plus(hours: 23),
                end: now.plus(hours: 25)
            )
            alert.activePeriod(
                start: now.plus(hours: 47),
                end: now.plus(hours: 49)
            )
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: [route], stop: stop, affectedStops: [stop], now: now)

        XCTAssertNotNil(try sut.inspect().find(text: "Daily"))
        XCTAssertNotNil(try sut.inspect().find(text: "Fri, Jan 23 – Sun, Jan 25"))
        XCTAssertNotNil(try sut.inspect().find(text: "From"))
        XCTAssertNotNil(try sut.inspect().find(text: "9:41\u{202F}AM – 11:41\u{202F}AM"))
    }

    func testRecurringUntilFurtherNotice() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant(year: 2026, month: .january, day: 23, hour: 10, minute: 41, second: 0)

        let route = objects.route { _ in }
        let stop = objects.stop { $0.name = "Park Street" }
        let alert = objects.alert { alert in
            alert.effect = .suspension
            alert.durationCertainty = .unknown
            alert.activePeriod(
                start: now.minus(hours: 1),
                end: now.plus(hours: 1)
            )
            alert.activePeriod(
                start: now.plus(hours: 23),
                end: now.plus(hours: 25)
            )
            alert.activePeriod(
                start: now.plus(hours: 47),
                end: now.plus(hours: 49)
            )
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: [route], stop: stop, affectedStops: [stop], now: now)

        XCTAssertNotNil(try sut.inspect().find(text: "Daily"))
        XCTAssertNotNil(try sut.inspect().find(text: "Until further notice"))
        XCTAssertNotNil(try sut.inspect().find(text: "From"))
        XCTAssertNotNil(try sut.inspect().find(text: "9:41\u{202F}AM – 11:41\u{202F}AM"))
    }

    func testRecurringSomeDays() throws {
        let objects = ObjectCollectionBuilder()

        let now = EasternTimeInstant(year: 2026, month: .january, day: 23, hour: 10, minute: 41, second: 0)

        let route = objects.route { _ in }
        let stop = objects.stop { $0.name = "Park Street" }
        let alert = objects.alert { alert in
            alert.effect = .suspension
            alert.activePeriod(
                start: now.minus(hours: 1),
                end: now.plus(hours: 1)
            )
            alert.activePeriod(
                start: now.plus(hours: 47),
                end: now.plus(hours: 49)
            )
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: [route], stop: stop, affectedStops: [stop], now: now)

        XCTAssertNotNil(try sut.inspect().find(text: "January 23 – January 25"))
        XCTAssertNotNil(try sut.inspect().find(text: "Sunday, Friday"))
        XCTAssertNotNil(try sut.inspect().find(text: "From"))
        XCTAssertNotNil(try sut.inspect().find(text: "9:41\u{202F}AM – 11:41\u{202F}AM"))
    }
}

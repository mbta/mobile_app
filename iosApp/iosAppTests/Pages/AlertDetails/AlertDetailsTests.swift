//
//  AlertDetailsTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 8/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
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

        let now = Date.now

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.addingTimeInterval(-5).toKotlinInstant(),
                end: now.addingTimeInterval(5).toKotlinInstant()
            )
            alert.description_ = "Long description"
            alert.cause = .unrulyPassenger
            alert.effect = .stopClosure
            alert.effectName = "Closure"
            alert.header = "Alert header"
            alert.updatedAt = now.addingTimeInterval(-100).toKotlinInstant()
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
        XCTAssertNotNil(try sut.inspect().find(
            text: "Updated: \(alert.updatedAt.toNSDate().formatted(date: .numeric, time: .shortened))"
        ))
    }

    func testCurrentActivePeriod() throws {
        let objects = ObjectCollectionBuilder()

        let now = Date.now

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.addingTimeInterval(-20).toKotlinInstant(),
                end: now.addingTimeInterval(-10).toKotlinInstant()
            )
            alert.activePeriod(
                start: now.addingTimeInterval(-5).toKotlinInstant(),
                end: now.addingTimeInterval(5).toKotlinInstant()
            )
            alert.activePeriod(
                start: now.addingTimeInterval(10).toKotlinInstant(),
                end: now.addingTimeInterval(20).toKotlinInstant()
            )
            alert.updatedAt = now.addingTimeInterval(-100).toKotlinInstant()
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, affectedStops: [], now: now)

        XCTAssertNotNil(try sut.inspect().find(text: String(alert.activePeriod[1].formatStart().characters)))
    }

    func testNoCurrentActivePeriod() throws {
        let objects = ObjectCollectionBuilder()

        let now = Date.now

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.addingTimeInterval(-10).toKotlinInstant(),
                end: now.addingTimeInterval(-5).toKotlinInstant()
            )
            alert.activePeriod(
                start: now.addingTimeInterval(5).toKotlinInstant(),
                end: now.addingTimeInterval(10).toKotlinInstant()
            )
            alert.cause = .unrulyPassenger
            alert.effect = .stopClosure
            alert.effectName = "Closure"
            alert.updatedAt = now.addingTimeInterval(-100).toKotlinInstant()
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, affectedStops: [], now: now)

        XCTAssertNotNil(try? sut.inspect().find(text: "Alert is no longer in effect"))
        XCTAssertNil(try? sut.inspect().find(text: "Start"))
    }

    func testNoDescription() throws {
        let objects = ObjectCollectionBuilder()

        let now = Date.now

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.addingTimeInterval(-5).toKotlinInstant(),
                end: now.addingTimeInterval(5).toKotlinInstant()
            )
            alert.cause = .unrulyPassenger
            alert.effect = .stopClosure
            alert.effectName = "Closure"
            alert.updatedAt = now.addingTimeInterval(-100).toKotlinInstant()
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, affectedStops: [], now: now)

        XCTAssertNil(try? sut.inspect().find(text: "Full Description"))
    }

    func testStopsInDescription() throws {
        let objects = ObjectCollectionBuilder()

        let now = Date.now

        let stop1 = objects.stop { stop in stop.name = "Stop 1" }
        let stop2 = objects.stop { stop in stop.name = "Stop 2" }
        let stop3 = objects.stop { stop in stop.name = "Stop 3" }

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.addingTimeInterval(-5).toKotlinInstant(),
                end: now.addingTimeInterval(5).toKotlinInstant()
            )
            alert.cause = .unrulyPassenger
            alert.effect = .stopClosure
            alert.effectName = "Closure"
            alert.updatedAt = now.addingTimeInterval(-100).toKotlinInstant()
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

        let now = Date.now

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.addingTimeInterval(-5).toKotlinInstant(),
                end: now.addingTimeInterval(5).toKotlinInstant()
            )
            alert.cause = .maintenance
            alert.effect = .elevatorClosure
            alert.updatedAt = now.addingTimeInterval(-100).toKotlinInstant()
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, stop: stop, affectedStops: [stop], now: now)

        XCTAssertNotNil(try sut.inspect().find(text: "Stop Elevator Closure"))
        XCTAssertNotNil(try sut.inspect().find(text: "Maintenance"))
    }

    func testEffectOnlyHeader() throws {
        let objects = ObjectCollectionBuilder()

        let now = Date.now

        let alert = objects.alert { alert in
            alert.activePeriod(
                start: now.addingTimeInterval(-5).toKotlinInstant(),
                end: now.addingTimeInterval(5).toKotlinInstant()
            )
            alert.cause = .freightTrainInterference
            alert.effect = .serviceChange
            alert.updatedAt = now.addingTimeInterval(-100).toKotlinInstant()
        }

        let sut = AlertDetails(alert: alert, line: nil, routes: nil, stop: nil, affectedStops: [], now: now)

        XCTAssertNotNil(try sut.inspect().find(text: "Service Change"))
    }

    func testElevatorClosure() throws {
        let objects = ObjectCollectionBuilder()

        let now = Date.now

        let route = objects.route { _ in }
        let stop = objects.stop { $0.name = "Park Street" }
        let alert = objects.alert { alert in
            alert.effect = .elevatorClosure
            alert.header = "Elevator 123 (Foo to Bar) unavailable due to demonstration"
            alert.cause = .demonstration
            alert.activePeriod(
                start: (now - 3 * 24 * 60 * 60).toKotlinInstant(),
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
            alert.updatedAt = (now - 10 * 60).toKotlinInstant()
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
}

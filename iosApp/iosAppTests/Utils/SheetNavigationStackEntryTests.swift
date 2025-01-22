//
//  SheetNavigationStackEntryTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-04-09.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import shared
import XCTest

final class SheetNavigationStackEntryTests: XCTestCase {
    func testLastFilterEmpty() throws {
        var stack: [SheetNavigationStackEntry] = []

        XCTAssertNil(stack.lastStopDetailsFilter)

        stack.lastStopDetailsFilter = .init(routeId: "A", directionId: 1)

        XCTAssertEqual(stack, [])
    }

    func testLastFilterShallowLegacy() throws {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        var stack: [SheetNavigationStackEntry] = [.legacyStopDetails(stop, nil)]

        XCTAssertNil(stack.lastStopDetailsFilter)

        stack.lastStopDetailsFilter = .init(routeId: "A", directionId: 1)

        XCTAssertEqual(stack, [.legacyStopDetails(stop, .init(routeId: "A", directionId: 1))])
        XCTAssertEqual(stack.lastStopDetailsFilter, .init(routeId: "A", directionId: 1))

        stack.lastStopDetailsFilter = nil

        XCTAssertEqual(stack, [.legacyStopDetails(stop, nil)])
        XCTAssertEqual(stack.lastStopDetailsFilter, nil)
    }

    func testLastFilterDeepLegacy() throws {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        let otherStop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        let previousEntries: [SheetNavigationStackEntry] = [
            .legacyStopDetails(otherStop, .init(routeId: "A", directionId: 1)),
            .legacyStopDetails(otherStop, .init(routeId: "B", directionId: 1)),
            .tripDetails(
                tripId: "345",
                vehicleId: "abc",
                target: .init(stopId: "999", stopSequence: 111),
                routeId: "Z",
                directionId: 1
            ),
            .legacyStopDetails(otherStop, .init(routeId: "C", directionId: 0)),
            .legacyStopDetails(otherStop, .init(routeId: "D", directionId: 0)),
        ]
        var stack: [SheetNavigationStackEntry] = previousEntries + [.legacyStopDetails(
            stop,
            .init(routeId: "E", directionId: 0)
        )]

        XCTAssertEqual(stack.lastStopDetailsFilter, .init(routeId: "E", directionId: 0))

        stack.lastStopDetailsFilter = nil

        XCTAssertEqual(stack, previousEntries + [.legacyStopDetails(stop, nil)])
        XCTAssertEqual(stack.lastStopDetailsFilter, nil)
    }

    func testLastFilterNotTopLegacy() throws {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        var stack: [SheetNavigationStackEntry] = [
            .legacyStopDetails(stop, .init(routeId: "A", directionId: 1)),
            .legacyStopDetails(stop, .init(routeId: "B", directionId: 0)),
            .tripDetails(tripId: "a", vehicleId: "1", target: nil, routeId: "C", directionId: 1),
        ]

        XCTAssertEqual(stack.lastStopDetailsFilter, nil)

        let prevStack = Array(stack)
        stack.lastStopDetailsFilter = nil

        XCTAssertEqual(stack, prevStack)
    }

    func testNavStackEntryIdentifables() throws {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        let stopEntry: SheetNavigationStackEntry = .legacyStopDetails(stop, .init(routeId: "A", directionId: 1))
        let tripEntry: SheetNavigationStackEntry = .tripDetails(
            tripId: "tripId",
            vehicleId: "vehicleId",
            target: nil,
            routeId: "routeId",
            directionId: 0
        )

        let nearbyEntry: SheetNavigationStackEntry = .nearby
        let alertEntry: SheetNavigationStackEntry = .alertDetails(alertId: "0", line: nil, routes: nil, stop: nil)

        XCTAssertEqual(stopEntry.sheetItemIdentifiable()!.id, stop.id)
        XCTAssertEqual(tripEntry.sheetItemIdentifiable()!.id, "tripId")
        XCTAssertEqual(nearbyEntry.sheetItemIdentifiable()!.id, "nearby")
        XCTAssertNil(alertEntry.sheetItemIdentifiable())

        XCTAssertNil(stopEntry.coverItemIdentifiable())
        XCTAssertNil(tripEntry.coverItemIdentifiable())
        XCTAssertNil(nearbyEntry.coverItemIdentifiable())
        XCTAssertEqual(alertEntry.coverItemIdentifiable()!.id, "0")
    }

    func testNavStackLastStopLegacy() throws {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        let stopEntry: SheetNavigationStackEntry = .legacyStopDetails(stop, .init(routeId: "A", directionId: 1))
        let tripEntry: SheetNavigationStackEntry = .tripDetails(
            tripId: "tripId",
            vehicleId: "vehicleId",
            target: nil,
            routeId: "routeId",
            directionId: 0
        )
        let alertEntry: SheetNavigationStackEntry = .alertDetails(alertId: "0", line: nil, routes: nil, stop: nil)

        var stack: [SheetNavigationStackEntry] = []
        XCTAssertNil(stack.lastStop)
        stack.append(.nearby)
        XCTAssertNil(stack.lastStop)
        stack.append(stopEntry)
        XCTAssertEqual(stop, stack.lastStop)
        stack.append(alertEntry)
        XCTAssertEqual(stop, stack.lastStop)
        stack.remove(at: stack.count - 1)
        stack.append(tripEntry)
        XCTAssertEqual(stop, stack.lastStop)
    }

    func testNavStackLastStopId() throws {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        let legacyStopEntry: SheetNavigationStackEntry = .legacyStopDetails(stop, .init(routeId: "A", directionId: 1))
        let stopEntry: SheetNavigationStackEntry = .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)
        let tripEntry: SheetNavigationStackEntry = .tripDetails(
            tripId: "tripId",
            vehicleId: "vehicleId",
            target: nil,
            routeId: "routeId",
            directionId: 0
        )
        let alertEntry: SheetNavigationStackEntry = .alertDetails(alertId: "0", line: nil, routes: nil, stop: nil)

        var stack: [SheetNavigationStackEntry] = []
        XCTAssertNil(stack.lastStopId)
        stack.append(.nearby)
        XCTAssertNil(stack.lastStopId)
        stack.append(legacyStopEntry)
        XCTAssertEqual(stop.id, stack.lastStopId)
        stack.append(alertEntry)
        XCTAssertEqual(stop.id, stack.lastStopId)
        stack.remove(at: stack.count - 1)
        stack.append(tripEntry)
        XCTAssertEqual(stop.id, stack.lastStopId)
        stack.removeAll()
        stack.append(stopEntry)
        XCTAssertEqual(stop.id, stack.lastStopId)
        stack.append(alertEntry)
        XCTAssertEqual(stop.id, stack.lastStopId)
        stack.remove(at: stack.count - 1)
        stack.append(tripEntry)
        XCTAssertEqual(stop.id, stack.lastStopId)
    }

    func testLastFilterSet() throws {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        var stack: [SheetNavigationStackEntry] = [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)]

        let filter1 = StopDetailsFilter(routeId: "A", directionId: 1)
        stack.lastStopDetailsFilter = filter1

        XCTAssertEqual([
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
            .stopDetails(stopId: stop.id, stopFilter: filter1, tripFilter: nil),
        ], stack)
        XCTAssertEqual(filter1, stack.lastStopDetailsFilter)

        let filter2 = StopDetailsFilter(routeId: "A", directionId: 0)
        stack.lastStopDetailsFilter = filter2

        XCTAssertEqual([
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
            .stopDetails(stopId: stop.id, stopFilter: filter2, tripFilter: nil),
        ], stack)
        XCTAssertEqual(filter2, stack.lastStopDetailsFilter)

        stack.lastStopDetailsFilter = nil

        XCTAssertEqual([.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)], stack)
        XCTAssertEqual(nil, stack.lastStopDetailsFilter)

        stack.lastStopDetailsFilter = nil

        XCTAssertEqual([.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)], stack)
        XCTAssertEqual(nil, stack.lastStopDetailsFilter)
    }
}

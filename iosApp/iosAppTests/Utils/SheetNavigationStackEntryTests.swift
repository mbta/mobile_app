//
//  SheetNavigationStackEntryTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-04-09.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import XCTest

final class SheetNavigationStackEntryTests: XCTestCase {
    func testLastFilterEmpty() throws {
        var stack: [SheetNavigationStackEntry] = []

        XCTAssertNil(stack.lastStopDetailsFilter)

        stack.lastStopDetailsFilter = .init(routeId: "A", directionId: 1)

        XCTAssertEqual(stack, [])
    }

    func testNavStackEntryIdentifables() throws {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        let stopEntry: SheetNavigationStackEntry = .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)
        let nearbyEntry: SheetNavigationStackEntry = .nearby
        let alertEntry: SheetNavigationStackEntry = .alertDetails(alertId: "0", line: nil, routes: nil, stop: nil)

        XCTAssertEqual(stopEntry.sheetItemIdentifiable()!.id, stop.id)
        XCTAssertEqual(nearbyEntry.sheetItemIdentifiable()!.id, "nearby")
        XCTAssertNil(alertEntry.sheetItemIdentifiable())

        XCTAssertNil(stopEntry.coverItemIdentifiable())
        XCTAssertNil(nearbyEntry.coverItemIdentifiable())
        XCTAssertEqual(alertEntry.coverItemIdentifiable()!.id, "0")
    }

    func testNavStackLastStopId() throws {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        let stopEntry: SheetNavigationStackEntry = .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)
        let alertEntry: SheetNavigationStackEntry = .alertDetails(alertId: "0", line: nil, routes: nil, stop: nil)

        var stack: [SheetNavigationStackEntry] = []
        XCTAssertNil(stack.lastStopId)
        stack.append(.nearby)
        XCTAssertNil(stack.lastStopId)
        stack.append(alertEntry)
        XCTAssertNil(stack.lastStopId)
        stack.append(stopEntry)
        XCTAssertEqual(stop.id, stack.lastStopId)

        stack.removeAll()
        stack.append(stopEntry)
        XCTAssertEqual(stop.id, stack.lastStopId)
        stack.append(alertEntry)
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

    func testToSheetRoute() {
        XCTAssertEqual(SheetRoutes.NearbyTransit(), SheetNavigationStackEntry.nearby.toSheetRoute())
        XCTAssertEqual(SheetRoutes.Favorites(), SheetNavigationStackEntry.favorites.toSheetRoute())
        XCTAssertEqual(SheetRoutes.EditFavorites(), SheetNavigationStackEntry.editFavorites.toSheetRoute())

        XCTAssertEqual(
            SheetRoutes.RoutePicker(path: .Bus(), context: .Favorites()),
            SheetNavigationStackEntry.routePicker(SheetRoutes.RoutePicker(path: .Bus(), context: .Favorites()))
                .toSheetRoute()
        )

        XCTAssertEqual(SheetRoutes.RouteDetails(routeId: "a", context: .Favorites()),
                       SheetNavigationStackEntry.routeDetails(SheetRoutes.RouteDetails(routeId: "a",
                                                                                       context: .Favorites()))
                           .toSheetRoute())

        XCTAssertEqual(SheetRoutes.StopDetails(stopId: "a",
                                               stopFilter: .init(routeId: "b", directionId: 0),
                                               tripFilter: .init(tripId: "c",
                                                                 vehicleId: "d",
                                                                 stopSequence: 0,
                                                                 selectionLock: false)),
                       SheetNavigationStackEntry.stopDetails(stopId: "a",
                                                             stopFilter: .init(routeId: "b", directionId: 0),
                                                             tripFilter: .init(tripId: "c",
                                                                               vehicleId: "d",
                                                                               stopSequence: 0,
                                                                               selectionLock: false)).toSheetRoute())

        XCTAssertNil(SheetNavigationStackEntry.more.toSheetRoute())
        XCTAssertNil(SheetNavigationStackEntry.alertDetails(alertId: "a", line: nil, routes: [], stop: nil)
            .toSheetRoute())
    }
}

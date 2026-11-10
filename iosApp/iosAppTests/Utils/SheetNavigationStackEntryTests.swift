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
    func testLastFilterEmpty() {
        var stack: [SheetNavigationStackEntry] = []

        XCTAssertNil(stack.lastRouteStopFilter)

        stack.lastRouteStopFilter = .init(routeId: Route.Id("A"), directionId: 1)

        XCTAssertEqual(stack, [])
    }

    func testNavStackEntryIdentifables() {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        let stopEntry: SheetNavigationStackEntry = .stopDetails(stopId: stop.id, routeStopFilter: nil, tripFilter: nil)
        let nearbyEntry: SheetNavigationStackEntry = .nearby
        let alertEntry: SheetNavigationStackEntry = .alertDetails(alertId: "0", line: nil, routes: nil, stop: nil)

        XCTAssertEqual(stopEntry.sheetItemIdentifiable()?.id, stop.id)
        XCTAssertEqual(nearbyEntry.sheetItemIdentifiable()?.id, "nearby")
        XCTAssertNil(alertEntry.sheetItemIdentifiable())

        XCTAssertNil(stopEntry.coverItemIdentifiable())
        XCTAssertNil(nearbyEntry.coverItemIdentifiable())
        XCTAssertEqual(alertEntry.coverItemIdentifiable()?.id, "0")
    }

    func testNavStackLastStopId() {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        let stopEntry: SheetNavigationStackEntry = .stopDetails(stopId: stop.id, routeStopFilter: nil, tripFilter: nil)
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

    func testLastFilterSet() {
        let stop = ObjectCollectionBuilder.Single.shared.stop { _ in }
        var stack: [SheetNavigationStackEntry] = [.stopDetails(stopId: stop.id, routeStopFilter: nil, tripFilter: nil)]

        let filter1 = RouteStopFilter(routeId: Route.Id("A"), directionId: 1)
        stack.lastRouteStopFilter = filter1

        XCTAssertEqual([
            .stopDetails(stopId: stop.id, routeStopFilter: nil, tripFilter: nil),
            .stopDetails(stopId: stop.id, routeStopFilter: filter1, tripFilter: nil),
        ], stack)
        XCTAssertEqual(filter1, stack.lastRouteStopFilter)

        let filter2 = RouteStopFilter(routeId: Route.Id("A"), directionId: 0)
        stack.lastRouteStopFilter = filter2

        XCTAssertEqual([
            .stopDetails(stopId: stop.id, routeStopFilter: nil, tripFilter: nil),
            .stopDetails(stopId: stop.id, routeStopFilter: filter2, tripFilter: nil),
        ], stack)
        XCTAssertEqual(filter2, stack.lastRouteStopFilter)

        stack.lastRouteStopFilter = nil

        XCTAssertEqual([.stopDetails(stopId: stop.id, routeStopFilter: nil, tripFilter: nil)], stack)
        XCTAssertEqual(nil, stack.lastRouteStopFilter)

        stack.lastRouteStopFilter = nil

        XCTAssertEqual([.stopDetails(stopId: stop.id, routeStopFilter: nil, tripFilter: nil)], stack)
        XCTAssertEqual(nil, stack.lastRouteStopFilter)
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

        XCTAssertEqual(SheetRoutes.RouteDetails(routeId: Route.Id("a"), context: .Favorites()),
                       SheetNavigationStackEntry.routeDetails(SheetRoutes.RouteDetails(routeId: Route.Id("a"),
                                                                                       context: .Favorites()))
                           .toSheetRoute())

        XCTAssertEqual(SheetRoutes.StopDetails(stopId: "a",
                                               routeStopFilter: .init(routeId: Route.Id("b"), directionId: 0),
                                               tripFilter: .init(tripId: "c",
                                                                 vehicleId: "d",
                                                                 stopSequence: 0,
                                                                 selectionLock: false)),
                       SheetNavigationStackEntry.stopDetails(stopId: "a",
                                                             routeStopFilter: .init(
                                                                 routeId: Route.Id("b"),
                                                                 directionId: 0
                                                             ),
                                                             tripFilter: .init(tripId: "c",
                                                                               vehicleId: "d",
                                                                               stopSequence: 0,
                                                                               selectionLock: false)).toSheetRoute())

        XCTAssertNil(SheetNavigationStackEntry.more(highlight: nil).toSheetRoute())
        XCTAssertNil(SheetNavigationStackEntry.alertDetails(alertId: "a", line: nil, routes: [], stop: nil)
            .toSheetRoute())
    }
}

//
//  NearbyViewModelTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 5/8/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import XCTest

final class NearbyViewModelTests: XCTestCase {
    func testIsNearbyVisibleWhenNoStack() {
        XCTAssert(NearbyViewModel(navigationStack: []).isNearbyVisible())
    }

    func testIsNearbyVisibleFalseWhenStack() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        XCTAssertFalse(NearbyViewModel(navigationStack: [
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
        ]).isNearbyVisible())
    }

    func testGoBackOnEmptyStack() {
        // This should succeed unless there's a fatal error thrown
        let nearbyVM: NearbyViewModel = .init(navigationStack: [])
        nearbyVM.goBack()
    }

    func testAppendNavStopDetails() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let entry0: SheetNavigationStackEntry = .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)
        let entry1: SheetNavigationStackEntry = .stopDetails(stopId: "other", stopFilter: nil, tripFilter: nil)

        let nearbyVM: NearbyViewModel = .init(navigationStack: [entry0])

        nearbyVM.appendNavEntry(entry1)
        XCTAssertEqual([entry0, entry1], nearbyVM.navigationStack)

        nearbyVM.appendNavEntry(entry1)
        XCTAssertEqual([entry0, entry1], nearbyVM.navigationStack)
    }

    func testStopDetailsNav() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let nearbyVM: NearbyViewModel = .init(navigationStack: [
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
        ])

        let filter1: StopDetailsFilter = .init(routeId: Route.Id("1"), directionId: 1)
        nearbyVM.pushNavEntry(.stopDetails(stopId: stop.id, stopFilter: filter1, tripFilter: nil))
        XCTAssertEqual([
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
            .stopDetails(stopId: stop.id, stopFilter: filter1, tripFilter: nil),
        ], nearbyVM.navigationStack)

        let filter2: StopDetailsFilter = .init(routeId: Route.Id("1"), directionId: 0)
        nearbyVM.pushNavEntry(.stopDetails(stopId: stop.id, stopFilter: filter2, tripFilter: nil))
        XCTAssertEqual([
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
            .stopDetails(stopId: stop.id, stopFilter: filter2, tripFilter: nil),
        ], nearbyVM.navigationStack)

        nearbyVM.pushNavEntry(.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil))
        XCTAssertEqual([
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
        ], nearbyVM.navigationStack)
    }

    func testStopDetailsNavPopsWhenAutoFilter() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let nearbyVM: NearbyViewModel = .init(navigationStack: [
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
        ])

        let filter1: StopDetailsFilter = .init(routeId: Route.Id("1"), directionId: 1, autoFilter: true)
        nearbyVM.pushNavEntry(.stopDetails(stopId: stop.id, stopFilter: filter1, tripFilter: nil))
        XCTAssertEqual([
            .stopDetails(stopId: stop.id, stopFilter: filter1, tripFilter: nil),
        ], nearbyVM.navigationStack)
    }

    func testSetLastStopDetailsFilter() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let nearbyVM: NearbyViewModel = .init(navigationStack: [
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
        ])

        let filter1: StopDetailsFilter = .init(routeId: Route.Id("1"), directionId: 1)
        nearbyVM.setLastStopDetailsFilter(stop.id, filter1)
        XCTAssertEqual([
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
            .stopDetails(stopId: stop.id, stopFilter: filter1, tripFilter: nil),
        ], nearbyVM.navigationStack)

        let filter2: StopDetailsFilter = .init(routeId: Route.Id("1"), directionId: 0)
        nearbyVM.setLastStopDetailsFilter(stop.id, filter2)
        XCTAssertEqual([
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
            .stopDetails(stopId: stop.id, stopFilter: filter2, tripFilter: nil),
        ], nearbyVM.navigationStack)

        nearbyVM.setLastStopDetailsFilter(stop.id, nil)
        XCTAssertEqual([
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
        ], nearbyVM.navigationStack)
    }
}

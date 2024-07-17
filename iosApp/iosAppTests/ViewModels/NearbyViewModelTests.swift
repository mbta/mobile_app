//
//  NearbyViewModelTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 5/8/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
import XCTest

final class NearbyViewModelTests: XCTestCase {
    func testIsNearbyVisibleWhenNoStack() {
        XCTAssert(NearbyViewModel(navigationStack: []).isNearbyVisible())
    }

    func testIsNearbyVisibleFalseWhenStack() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        XCTAssertFalse(NearbyViewModel(navigationStack: [.stopDetails(stop, nil)]).isNearbyVisible())
    }

    func testPushEntryAddsToStackWhenDifferentStops() {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { _ in }
        let stop2 = objects.stop { _ in }
        let nearbyVM: NearbyViewModel = .init(navigationStack: [])

        let entry1: SheetNavigationStackEntry = .stopDetails(stop1, .init(routeId: "Route1", directionId: 0))
        let entry2: SheetNavigationStackEntry = .stopDetails(stop2, .init(routeId: "Route2", directionId: 1))

        nearbyVM.pushNavEntry(entry1)
        XCTAssertEqual(nearbyVM.navigationStack, [entry1])
        nearbyVM.pushNavEntry(entry2)
        XCTAssertEqual(nearbyVM.navigationStack, [entry1, entry2])
    }

    func testPushEntrySetsLastStopWhenSameStop() {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { _ in }
        let nearbyVM: NearbyViewModel = .init(navigationStack: [])

        let entry1: SheetNavigationStackEntry = .stopDetails(stop1, .init(routeId: "Route1", directionId: 0))
        let entry2: SheetNavigationStackEntry = .stopDetails(stop1, .init(routeId: "Route2", directionId: 1))

        nearbyVM.pushNavEntry(entry1)
        XCTAssertEqual(nearbyVM.navigationStack, [entry1])
        nearbyVM.pushNavEntry(entry2)
        XCTAssertEqual(nearbyVM.navigationStack, [entry2])
    }

    func testTargetStop() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let nearbyVM: NearbyViewModel = .init(navigationStack: [])

        let mockGlobal = GlobalResponse(objects: objects, patternIdsByStop: [:])

        nearbyVM.pushNavEntry(.nearby)

        XCTAssertEqual(nearbyVM.getTargetStop(global: mockGlobal), nil)

        let stopEntry: SheetNavigationStackEntry = .stopDetails(stop, .init(routeId: "Route1", directionId: 0))
        nearbyVM.pushNavEntry(stopEntry)

        XCTAssertEqual(nearbyVM.getTargetStop(global: mockGlobal), stop)

        let tripEntry: SheetNavigationStackEntry = .tripDetails(
            tripId: "", vehicleId: "", target: .init(stopId: stop.id, stopSequence: nil), routeId: "", directionId: 0
        )
        nearbyVM.pushNavEntry(tripEntry)

        XCTAssertEqual(nearbyVM.getTargetStop(global: mockGlobal), stop)
    }
}

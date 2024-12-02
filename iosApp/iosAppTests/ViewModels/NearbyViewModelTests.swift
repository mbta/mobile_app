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
        XCTAssertFalse(NearbyViewModel(navigationStack: [.legacyStopDetails(stop, nil)]).isNearbyVisible())
    }

    func testPushEntryAddsToStackWhenDifferentStops() {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { _ in }
        let stop2 = objects.stop { _ in }
        let nearbyVM: NearbyViewModel = .init(navigationStack: [])

        let entry1: SheetNavigationStackEntry = .legacyStopDetails(stop1, .init(routeId: "Route1", directionId: 0))
        let entry2: SheetNavigationStackEntry = .legacyStopDetails(stop2, .init(routeId: "Route2", directionId: 1))

        nearbyVM.pushNavEntry(entry1)
        XCTAssertEqual(nearbyVM.navigationStack, [entry1])
        nearbyVM.pushNavEntry(entry2)
        XCTAssertEqual(nearbyVM.navigationStack, [entry1, entry2])
    }

    func testPushEntrySetsLastStopWhenSameStop() {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { _ in }
        let nearbyVM: NearbyViewModel = .init(navigationStack: [])

        let entry1: SheetNavigationStackEntry = .legacyStopDetails(stop1, .init(routeId: "Route1", directionId: 0))
        let entry2: SheetNavigationStackEntry = .legacyStopDetails(stop1, .init(routeId: "Route2", directionId: 1))

        nearbyVM.pushNavEntry(entry1)
        XCTAssertEqual(nearbyVM.navigationStack, [entry1])
        nearbyVM.pushNavEntry(entry2)
        XCTAssertEqual(nearbyVM.navigationStack, [entry2])
    }

    func testSetLastStopDetailsFilterWhenIsLast() {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { _ in }
        let newFilter: StopDetailsFilter = .init(routeId: "2", directionId: 1)
        let nearbyVM: NearbyViewModel = .init(navigationStack: [.legacyStopDetails(
            stop1,
            StopDetailsFilter(routeId: "1",
                              directionId: 0)
        )])
        nearbyVM.setLastStopDetailsFilter(stop1.id, newFilter)
        XCTAssertEqual(nearbyVM.navigationStack.last, .legacyStopDetails(stop1, newFilter))
    }

    func testSetLastStopDetailsFilterWhenIsNotLast() {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { _ in }
        let stop2 = objects.stop { _ in }
        let newFilter: StopDetailsFilter = .init(routeId: "2", directionId: 1)
        let nearbyVM: NearbyViewModel = .init(navigationStack: [
            .legacyStopDetails(stop1, StopDetailsFilter(routeId: "1",
                                                        directionId: 0)),
            .legacyStopDetails(stop2, nil),
        ])
        nearbyVM.setLastStopDetailsFilter(stop1.id, newFilter)
        XCTAssertEqual(nearbyVM.navigationStack.last, .legacyStopDetails(stop2, nil))
    }

    func testSetDeparturesWhenIsLast() {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { _ in }
        let route1 = objects.route { _ in }
        let departures: StopDetailsDepartures = .init(routes: [
            .init(route: route1, stop: stop1, patterns: []),
        ])

        let nearbyVM: NearbyViewModel = .init(navigationStack: [.legacyStopDetails(stop1, nil)])
        nearbyVM.setDepartures(stop1.id, departures)
        XCTAssertEqual(nearbyVM.departures, departures)
    }

    func testSetDeparturesWhenIsNotLast() {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { _ in }
        let stop2 = objects.stop { _ in }
        let route1 = objects.route { _ in }
        let departures: StopDetailsDepartures = .init(routes: [
            .init(route: route1, stop: stop1, patterns: []),
        ])

        let nearbyVM: NearbyViewModel = .init(navigationStack: [.legacyStopDetails(stop1, nil),
                                                                .legacyStopDetails(stop2, nil)])
        nearbyVM.setDepartures(stop1.id, departures)
        XCTAssertEqual(nearbyVM.departures, nil)
    }

    func testTargetStop() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let nearbyVM: NearbyViewModel = .init(navigationStack: [])

        let mockGlobal = GlobalResponse(objects: objects, patternIdsByStop: [:])

        nearbyVM.pushNavEntry(.nearby)

        XCTAssertEqual(nearbyVM.getTargetStop(global: mockGlobal), nil)

        let stopEntry: SheetNavigationStackEntry = .legacyStopDetails(stop, .init(routeId: "Route1", directionId: 0))
        nearbyVM.pushNavEntry(stopEntry)

        XCTAssertEqual(nearbyVM.getTargetStop(global: mockGlobal), stop)

        let tripEntry: SheetNavigationStackEntry = .tripDetails(
            tripId: "", vehicleId: "", target: .init(stopId: stop.id, stopSequence: nil), routeId: "", directionId: 0
        )
        nearbyVM.pushNavEntry(tripEntry)

        XCTAssertEqual(nearbyVM.getTargetStop(global: mockGlobal), stop)
    }

    func testGoBackOnEmptyStack() {
        // This should succeed unless there's a fatal error thrown
        let nearbyVM: NearbyViewModel = .init(navigationStack: [])
        nearbyVM.goBack()
    }

    func testVisitHistoryChanges() async {
        let objects = ObjectCollectionBuilder()
        let stopA = objects.stop { _ in }
        let stopB = objects.stop { _ in }
        let stopC = objects.stop { _ in }

        let visitHistoryRepo = MockVisitHistoryRepository()
        let visitHistoryUsecase = VisitHistoryUsecase(repository: visitHistoryRepo)
        let nearbyVM: NearbyViewModel = .init(visitHistoryUsecase: visitHistoryUsecase)

        do {
            func pause() async throws { try await Task.sleep(nanoseconds: 200_000_000) }

            // Pause after every nav stack update to allow for the didSet to run
            nearbyVM.pushNavEntry(.legacyStopDetails(stopA, nil))
            try await pause()
            nearbyVM.navigationStack.removeAll()
            try await pause()
            nearbyVM.pushNavEntry(.legacyStopDetails(stopB, nil))
            try await pause()
            nearbyVM.pushNavEntry(.tripDetails(tripId: "", vehicleId: "", target: nil, routeId: "", directionId: 0))
            try await pause()
            nearbyVM.pushNavEntry(.legacyStopDetails(stopC, StopDetailsFilter(routeId: "route", directionId: 1)))
            try await pause()
            nearbyVM.navigationStack.removeAll()
            try await pause()
            nearbyVM.pushNavEntry(.legacyStopDetails(stopB, nil))
            try await pause()

            let visits = try await visitHistoryUsecase.getLatestVisits()
            XCTAssertEqual(
                [
                    Visit.StopVisit(stopId: stopB.id),
                    Visit.StopVisit(stopId: stopC.id),
                    Visit.StopVisit(stopId: stopA.id),
                ],
                visits
            )
        } catch {
            XCTFail("Getting latest visits failed, \(error)")
        }
    }
}

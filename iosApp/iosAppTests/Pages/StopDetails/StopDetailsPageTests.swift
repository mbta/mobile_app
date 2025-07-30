//
//  StopDetailsPageTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 4/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
@_spi(Experimental) import MapboxMaps
import Shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsPageTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testDisplaysSchedules() async throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let trip = objects.trip { _ in }
        objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id
            schedule.stopId = stop.id
            schedule.departureTime = EasternTimeInstant.now().plus(minutes: 10)
        }
        let routePattern = objects.routePattern(route: route) { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let stopFilter: StopDetailsFilter? = .init(
            routeId: route.id,
            directionId: routePattern.directionId
        )

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects))
        )

        stopDetailsVM.stopData = .init(
            stopId: stop.id,
            schedules: .init(objects: objects),
            predictionsByStop: .companion.empty,
            predictionsLoaded: true
        )

        let sut = StopDetailsPage(
            filters: .init(
                stopId: stop.id,
                stopFilter: stopFilter,
                tripFilter: nil
            ),
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))
        sut.updateDepartures()

        try await Task.sleep(for: .seconds(1))
        XCTAssertNotNil(try sut.inspect().find(StopDetailsFilteredView.self))
        XCTAssertNotNil(try sut.inspect().find(DepartureTile.self))
    }

    func testCloseButton() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let nearbyVM = NearbyViewModel(
            navigationStack: [
                .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
                .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
                .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
            ]
        )

        let stopDetailsVM = StopDetailsViewModel(
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository()
        )

        let sut = StopDetailsPage(
            filters: .init(
                stopId: stop.id,
                stopFilter: nil,
                tripFilter: nil
            ),
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: .init()
        ).withFixedSettings([:])

        XCTAssertEqual(3, nearbyVM.navigationStack.count)

        try sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()
        XCTAssertEqual(2, nearbyVM.navigationStack.count)
    }

    func testRejoinsPredictionsAfterBackgrounding() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let stopFilter: StopDetailsFilter? = .init(routeId: route.id, directionId: 0)
        let joinExpectation = expectation(description: "joins predictions")
        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(
            onConnect: {},
            onConnectV2: { _ in joinExpectation.fulfill() },
            onDisconnect: { leaveExpectation.fulfill() },
            connectOutcome: nil,
            connectV2Outcome: nil
        )

        let stopDetailsVM = StopDetailsViewModel(
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository()
        )

        let sut = StopDetailsPage(
            filters: .init(
                stopId: stop.id,
                stopFilter: stopFilter,
                tripFilter: nil
            ),
            errorBannerVM: .init(),
            nearbyVM: .init(),
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().findAndCallOnChange(newValue: ScenePhase.background)
        wait(for: [leaveExpectation], timeout: 1)

        try sut.inspect().findAndCallOnChange(newValue: ScenePhase.active)
        wait(for: [joinExpectation], timeout: 1)
    }

    @MainActor
    func testUpdatesRouteCardDataOnPredictionsChange() async throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }
        let prediction = objects.prediction { _ in }
        let pattern = objects.routePattern(route: route) { _ in }
        objects.trip { trip in
            trip.id = prediction.tripId
            trip.stopIds = [stop.id]
        }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let stopFilter: StopDetailsFilter? = .init(routeId: route.id, directionId: 0)

        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)]
        )
        nearbyVM.alerts = .init(alerts: [:])

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(),
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository()
        )
        stopDetailsVM.global = .init(objects: objects, patternIdsByStop: [stop.id: [pattern.id]])
        stopDetailsVM.stopData = nil

        let sut = StopDetailsPage(
            filters: .init(
                stopId: stop.id,
                stopFilter: stopFilter,
                tripFilter: nil
            ),
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        XCTAssertNil(nearbyVM.routeCardData)

        ViewHosting.host(view: sut.withFixedSettings([:]))
        stopDetailsVM.stopData = .init(
            stopId: stop.id,
            schedules: .init(objects: objects),
            predictionsByStop: .init(objects: objects),
            predictionsLoaded: true
        )

        let hasSetDepartures = sut.inspection.inspect(after: 1) { view in
            XCTAssertNotNil(nearbyVM.routeCardData)
            // Keeps internal departures in sync with VM departures
            XCTAssertEqual(try view.actualView().internalRouteCardData, nearbyVM.routeCardData)
        }

        await fulfillment(of: [hasSetDepartures], timeout: 2)
    }

    @MainActor
    func testUpdatesRouteCardDataWhenParamsChange() async throws {
        let objects = ObjectCollectionBuilder()

        let route = objects.route()
        let stop = objects.stop { _ in }

        let tripId = "trip"
        let routePattern = objects.routePattern(route: route) { pattern in
            pattern.representativeTripId = tripId
        }
        let trip = objects.trip(routePattern: routePattern) { trip in
            trip.id = tripId
            trip.directionId = 0
            trip.stopIds = [stop.id]
            trip.routePatternId = routePattern.id
        }
        objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id
            schedule.stopId = stop.id
            schedule.stopSequence = 0
            schedule.departureTime = EasternTimeInstant.now().plus(minutes: 10)
        }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)]
        )
        nearbyVM.alerts = .init(alerts: [:])

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Response: .init(objects: objects)),
            schedulesRepository: MockScheduleRepository(
                scheduleResponse: .init(objects: objects),
                callback: { _ in }
            )
        )

        stopDetailsVM.global = .init(objects: objects)
        stopDetailsVM.stopData = .init(
            stopId: stop.id,
            schedules: .init(objects: objects),
            predictionsByStop: .init(objects: objects),
            predictionsLoaded: true
        )

        let sut = StopDetailsPage(
            filters: .init(
                stopId: stop.id,
                stopFilter: nil,
                tripFilter: nil
            ),
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        let updatePublisher = PassthroughSubject<Void, Never>()

        var nowLater: Date?

        // unfortunately, round tripping Swift Dates to EasternTimeInstants can drift by a millisecond or so
        func roundDate(_ date: Date?) -> Date? {
            guard let date else { return nil }
            return Date(timeIntervalSince1970: round(date.timeIntervalSince1970))
        }

        let onChangeCalledExp = sut.inspection.inspect(after: 1) { view in
            nowLater = Date.now
            XCTAssertLessThan(nearbyVM.routeCardData!.first!.at.toNSDateLosingTimeZone(), nowLater!)
            XCTAssertEqual(nearbyVM.routeCardData, try view.actualView().internalRouteCardData)

            try view.findAndCallOnChange(newValue: RouteCardParams(
                alerts: nearbyVM.alerts,
                global: stopDetailsVM.global,
                now: nowLater!,
                pinnedRoutes: [],
                stopData: stopDetailsVM.stopData,
                stopFilter: nil,
                stopId: stop.id
            ))
            updatePublisher.send()
        }

        let routeCardDataSetExp = sut.inspection.inspect(onReceive: updatePublisher, after: 1) { view in
            XCTAssertEqual(roundDate(nowLater!), roundDate(nearbyVM.routeCardData?.first?.at.toNSDateLosingTimeZone()))
            XCTAssertEqual(
                roundDate(nowLater!),
                try roundDate(view.actualView().internalRouteCardData?.first?.at.toNSDateLosingTimeZone())
            )
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        await fulfillment(of: [onChangeCalledExp, routeCardDataSetExp], timeout: 3)
    }

    @MainActor
    func testAppliesStopFilterAutomatically() async throws {
        let objects = ObjectCollectionBuilder()

        let route = objects.route()
        let stop = objects.stop { _ in }

        let tripId = "trip"
        let routePattern = objects.routePattern(route: route) { pattern in
            pattern.representativeTripId = tripId
        }
        let trip = objects.trip(routePattern: routePattern) { trip in
            trip.id = tripId
            trip.directionId = 0
            trip.stopIds = [stop.id]
            trip.routePatternId = routePattern.id
        }
        objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id
            schedule.stopId = stop.id
            schedule.stopSequence = 0
            schedule.departureTime = EasternTimeInstant.now().plus(minutes: 10)
        }

        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil)]
        )
        nearbyVM.alerts = .init(alerts: [:])

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Response: .init(objects: objects)),
            schedulesRepository: MockScheduleRepository(
                scheduleResponse: .init(objects: objects),
                callback: { _ in }
            )
        )

        stopDetailsVM.global = .init(objects: objects)
        stopDetailsVM.stopData = .init(
            stopId: stop.id,
            schedules: .init(objects: objects),
            predictionsByStop: .init(objects: objects),
            predictionsLoaded: true
        )

        let sut = StopDetailsPage(
            filters: .init(
                stopId: stop.id,
                stopFilter: nil,
                tripFilter: nil
            ),
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )
        ViewHosting.host(view: sut.withFixedSettings([:]))

        sut.updateDepartures()

        try await Task.sleep(for: .seconds(1))
        XCTAssertEqual(
            nearbyVM.navigationStack.lastStopDetailsFilter,
            StopDetailsFilter(routeId: route.id, directionId: routePattern.directionId, autoFilter: true)
        )
    }

    @MainActor
    func testAppliesTripFilterAutomatically() async throws {
        let objects = ObjectCollectionBuilder()

        let route = objects.route()
        let stop = objects.stop { _ in }

        let tripId = "trip"
        let routePattern = objects.routePattern(route: route) { pattern in
            pattern.representativeTripId = tripId
        }
        let trip = objects.trip(routePattern: routePattern) { trip in
            trip.id = tripId
            trip.directionId = 0
            trip.stopIds = [stop.id]
            trip.routePatternId = routePattern.id
        }
        let schedule = objects.schedule { schedule in
            schedule.trip = trip
            schedule.routeId = route.id
            schedule.stopId = stop.id
            schedule.stopSequence = 0
            schedule.departureTime = EasternTimeInstant.now().plus(minutes: 10)
        }
        objects.prediction(schedule: schedule) { prediction in
            prediction.departureTime = EasternTimeInstant.now().plus(minutes: 10)
        }

        let stopFilter: StopDetailsFilter = .init(routeId: route.id, directionId: 0)
        let viewportProvider: ViewportProvider = .init(viewport: .followPuck(zoom: 1))
        let nearbyVM: NearbyViewModel = .init(
            navigationStack: [.stopDetails(stopId: stop.id, stopFilter: stopFilter, tripFilter: nil)]
        )
        nearbyVM.alerts = .init(alerts: [:])

        let stopDetailsVM = StopDetailsViewModel(
            globalRepository: MockGlobalRepository(response: .init(objects: objects)),
            predictionsRepository: MockPredictionsRepository(connectV2Response: .init(objects: objects)),
            schedulesRepository: MockScheduleRepository(scheduleResponse: .init(objects: objects), callback: { _ in })
        )

        stopDetailsVM.global = .init(objects: objects)
        stopDetailsVM.stopData = .init(
            stopId: stop.id,
            schedules: .init(objects: objects),
            predictionsByStop: .init(objects: objects),
            predictionsLoaded: true
        )

        let sut = StopDetailsPage(
            filters: .init(
                stopId: stop.id,
                stopFilter: stopFilter,
                tripFilter: nil
            ),
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            mapVM: .init(),
            stopDetailsVM: stopDetailsVM,
            viewportProvider: viewportProvider
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        sut.updateDepartures()
        try await Task.sleep(for: .seconds(1))

        XCTAssertEqual(
            nearbyVM.navigationStack.lastTripDetailsFilter,
            TripDetailsFilter(tripId: trip.id, vehicleId: nil, stopSequence: 0, selectionLock: false)
        )
    }
}

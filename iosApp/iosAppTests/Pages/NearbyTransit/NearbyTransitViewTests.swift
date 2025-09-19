//
//  NearbyTransitViewTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-01-22.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
@testable import iosApp
@_spi(Experimental) import MapboxMaps
import Shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

// swiftlint:disable:next type_body_length
final class NearbyTransitViewTests: XCTestCase {
    private let noNearbyStops = { NoNearbyStopsView(onOpenSearch: {}, onPanToDefaultCenter: {}) }
    private var cancellables = Set<AnyCancellable>()

    class FakeNearbyVM: NearbyViewModel {
        let expectation: XCTestExpectation
        let closure: (CLLocationCoordinate2D) -> Void

        init(_ expectation: XCTestExpectation, _ closure: @escaping (CLLocationCoordinate2D) -> Void = { _ in }) {
            self.expectation = expectation
            self.closure = closure
            super.init()
        }

        override func getNearbyStops(global _: GlobalResponse, location: CLLocationCoordinate2D) {
            debugPrint("ViewModel getting nearby")
            closure(location)
            expectation.fulfill()
        }
    }

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testPending() throws {
        let sut = NearbyTransitView(
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            location: .constant(ViewportProvider.Defaults.center),
            setIsReturningFromBackground: { _ in },
            nearbyVM: .init(),
            noNearbyStops: noNearbyStops
        ).withFixedSettings([:])
        let cards = try sut.inspect().findAll(RouteCard.self)
        XCTAssertEqual(cards.count, 5)
        for card in cards {
            XCTAssertNotNil(try card.modifier(LoadingPlaceholderModifier.self))
        }
    }

    func testLoading() throws {
        let getNearbyExpectation = expectation(description: "getNearby")

        var sut = NearbyTransitView(
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            location: .constant(ViewportProvider.Defaults.center),
            setIsReturningFromBackground: { _ in },
            globalData: .init(objects: .init()),
            nearbyVM: FakeNearbyVM(getNearbyExpectation),
            noNearbyStops: noNearbyStops
        )

        let hasAppeared = sut.on(\.didAppear) { view in
            let cards = view.findAll(RouteCard.self)
            XCTAssertEqual(cards.count, 5)
            for card in cards {
                XCTAssertNotNil(try card.modifier(LoadingPlaceholderModifier.self))
            }
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared], timeout: 5)
        wait(for: [getNearbyExpectation], timeout: 5)
    }

    var mockLocation = CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)

    func testSchedulesFetchedOnAppear() throws {
        let objects = ObjectCollectionBuilder()
        objects.route { _ in }
        objects.stop { _ in }

        let schedulesFetchedExp = XCTestExpectation(description: "Schedules fetched")

        let sut = NearbyTransitView(
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(scheduleResponse: ScheduleResponse(objects: objects),
                                                        callback: { _ in schedulesFetchedExp.fulfill() }),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            globalData: .init(objects: objects),
            nearbyVM: NearbyViewModel(),
            scheduleResponse: .init(objects: objects),
            now: Date.now,
            predictionsByStop: .init(objects: objects),
            noNearbyStops: noNearbyStops
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [schedulesFetchedExp], timeout: 2)
    }

    @MainActor func testWithPredictions() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
        let now = EasternTimeInstant.now()
        let distantMinutes = 10
        let objects = TestData.clone()

        let route: RouteCardData.LineOrRoute = .route(TestData.getRoute(id: "67"))
        let stop = objects.getStop(id: "141")
        let trip = objects.getTrip(id: "68596786")
        let prediction = objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: Int32(distantMinutes))
            prediction.departureTime = now.plus(minutes: Int32(distantMinutes + 2))
            prediction.routeId = "67"
            prediction.stopId = "141"
            prediction.tripId = "68596786"
        }

        let nearbyVM = NearbyViewModel()
        nearbyVM.routeCardData = [.init(lineOrRoute: route,
                                        stopData: [.init(lineOrRoute: route,
                                                         stop: stop,
                                                         data: [.init(lineOrRoute: route,
                                                                      stop: stop,
                                                                      directionId: 0,
                                                                      routePatterns: [
                                                                          TestData.getRoutePattern(id: "67-4-0"),
                                                                      ],
                                                                      stopIds: ["141"],
                                                                      upcomingTrips: [.init(trip: trip,
                                                                                            prediction: prediction)],
                                                                      alertsHere: [],
                                                                      allDataLoaded: true,
                                                                      hasSchedulesToday: true,
                                                                      alertsDownstream: [],
                                                                      context: .nearbyTransit)],
                                                         globalData: GlobalResponse(objects: objects))],
                                        at: now)]

        let sut = NearbyTransitView(
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            globalData: .init(objects: objects),
            nearbyVM: nearbyVM,
            scheduleResponse: .init(objects: objects),
            now: now.toNSDateLosingTimeZone(),
            predictionsByStop: .init(objects: objects),
            noNearbyStops: noNearbyStops
        ).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(text: "67"))
        XCTAssertNotNil(try sut.inspect().find(text: "Alewife"))

        XCTAssertNotNil(try sut.inspect().find(text: "10 min"))
    }

    func testRefetchesPredictionsOnNewStops() throws {
        let now = EasternTimeInstant.now()

        let objects = TestData.clone()
        let davisExp = expectation(description: "joins predictions for Davis")
        let alewifeExp = expectation(description: "joins predictions for Alewife")

        let nearbyVM = NearbyViewModel()
        nearbyVM.nearbyState = .init(loadedLocation: mockLocation, loading: false, stopIds: ["place-davis"])
        nearbyVM.routeCardData = []

        let sut = NearbyTransitView(
            predictionsRepository: MockPredictionsRepository(onConnectV2: { stopIds in
                if stopIds == ["place-davis"] {
                    davisExp.fulfill()
                }
                if stopIds == ["place-alfcl"] {
                    alewifeExp.fulfill()
                }
            }),
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            globalData: .init(objects: objects),
            nearbyVM: nearbyVM,
            scheduleResponse: .init(objects: objects),
            now: now.toNSDateLosingTimeZone(),
            predictionsByStop: .init(objects: objects),
            noNearbyStops: noNearbyStops
        ).withFixedSettings([:])

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [davisExp], timeout: 2)

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ["place-alfcl"])

        wait(for: [alewifeExp], timeout: 2)
    }

    func testDoesntRefetchPredictionsOnStopReorder() throws {
        let now = EasternTimeInstant.now()

        let objects = TestData.clone()
        let initialJoinExp = expectation(description: "joins predictions")
        let reorderedJoinExp = expectation(description: "Doesn't rejoin on reorder")
        reorderedJoinExp.isInverted = true

        let nearbyVM = NearbyViewModel()
        nearbyVM.nearbyState = .init(loadedLocation: mockLocation,
                                     loading: false,
                                     stopIds: ["place-davis", "place-alfcl"])
        nearbyVM.routeCardData = []

        let sut = NearbyTransitView(
            predictionsRepository: MockPredictionsRepository(onConnectV2: { stopIds in
                if stopIds == ["place-davis", "place-alfcl"] {
                    initialJoinExp.fulfill()
                }
                if stopIds == ["place-alfcl", "place-davis"] {
                    reorderedJoinExp.fulfill()
                }
            }),
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            globalData: .init(objects: objects),
            nearbyVM: nearbyVM,
            scheduleResponse: .init(objects: objects),
            now: now.toNSDateLosingTimeZone(),
            predictionsByStop: .init(objects: objects),
            noNearbyStops: noNearbyStops
        ).withFixedSettings([:])

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [initialJoinExp], timeout: 2)

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ["place-alfcl", "place-davis"])

        wait(for: [reorderedJoinExp], timeout: 2)
    }

    func testFetchesPredictionsWhenNoStops() throws {
        let joinsPredictionsExpectation = expectation(description: "joins predictions")

        let objects = ObjectCollectionBuilder()

        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(objects: objects)

        var sut = NearbyTransitView(
            predictionsRepository: MockPredictionsRepository(
                onConnectV2: { _ in joinsPredictionsExpectation.fulfill() },
                connectV2Response: .init(objects: objects)
            ),
            schedulesRepository: MockScheduleRepository(scheduleResponse: .init(objects: objects)),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            globalData: .init(objects: objects),
            nearbyVM: nearbyVM,
            scheduleResponse: .init(objects: objects),
            now: Date.now,
            predictionsByStop: .init(objects: objects),
            noNearbyStops: noNearbyStops
        )

        let hasAppeared = sut.on(\.didAppear) { view in
            let cards = view.findAll(RouteCard.self)
            XCTAssertEqual(cards.count, 5)
            for card in cards {
                XCTAssertNotNil(try card.modifier(LoadingPlaceholderModifier.self))
            }
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [hasAppeared], timeout: 1)

        nearbyVM.nearbyState = .init(loadedLocation: mockLocation, loading: false, stopIds: [])

        wait(for: [joinsPredictionsExpectation], timeout: 1)
    }

    @MainActor func testLoadsRouteCardDataOnPredictionChange() throws {
        NSTimeZone.default = TimeZone(identifier: "America/New_York")!
        let loadRouteCardExp = XCTestExpectation(description: "loadRouteCard called")

        let objects = TestData.clone()

        class MockNearbyVM: NearbyViewModel {
            var onLoadRouteCard: () -> Void = {}

            override func loadRouteCardData(state _: NearbyViewModel.NearbyTransitState,
                                            global _: GlobalResponse?,
                                            schedules _: ScheduleResponse?,
                                            predictions _: PredictionsByStopJoinResponse?,
                                            alerts _: AlertsStreamDataResponse?,
                                            now _: Date) {
                onLoadRouteCard()
            }
        }

        let nearbyVM = MockNearbyVM()
        nearbyVM.onLoadRouteCard = { loadRouteCardExp.fulfill() }
        nearbyVM.nearbyState = .init(loadedLocation: mockLocation,
                                     loading: false,
                                     stopIds: ["141"])
        nearbyVM.routeCardData = []

        let sut = NearbyTransitView(
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            globalData: .init(objects: objects),
            nearbyVM: nearbyVM,
            scheduleResponse: .init(objects: objects),
            now: Date.now,
            predictionsByStop: .init(objects: objects),
            noNearbyStops: noNearbyStops
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        objects.prediction { prediction in
            prediction.id = "prediction"
            prediction.arrivalTime = EasternTimeInstant.now().plus(minutes: 2)
            prediction.departureTime = EasternTimeInstant.now().plus(minutes: 2)
            prediction.routeId = "67"
            prediction.stopId = "141"
            prediction.tripId = "tripId"
        }

        func changeParams(objects: ObjectCollectionBuilder) -> NearbyTransitView.RouteCardParams {
            .init(
                state: .init(loadedLocation: mockLocation, loading: false, stopIds: []),
                global: .init(objects: objects),
                schedules: .init(objects: objects),
                predictions: .init(objects: objects),
                alerts: .init(objects: objects),
                now: Date.now,
            )
        }

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: changeParams(objects: objects))
        wait(for: [loadRouteCardExp], timeout: 2)
    }

    func testLeavesChannelWhenBackgrounded() throws {
        let joinExpectation = expectation(description: "joins predictions")
        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(
            onConnectV2: { _ in joinExpectation.fulfill() },
            onDisconnect: { leaveExpectation.fulfill() }
        )
//        let objects = TestData.clone()
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        nearbyVM.nearbyState = .init(loadedLocation: mockLocation, loading: false, stopIds: [])
        let sut = NearbyTransitView(
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [joinExpectation], timeout: 1)
        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)
    }

    func testLeavesChannelWhenInactive() throws {
        let joinExpectation = expectation(description: "joins predictions")
        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(
            onConnectV2: { _ in joinExpectation.fulfill() },
            onDisconnect: { leaveExpectation.fulfill() }
        )

//        let objects = TestData.clone()
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        nearbyVM.nearbyState = .init(loadedLocation: mockLocation, loading: false, stopIds: [])
        let sut = NearbyTransitView(
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [joinExpectation], timeout: 1)
        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)
    }

    func testRejoinsChannelWhenReactivated() throws {
        let joinExpectation = expectation(description: "joins predictions")
        joinExpectation.expectedFulfillmentCount = 2
        joinExpectation.assertForOverFulfill = true

        let leaveExpectation = expectation(description: "leaves predictions")

        let predictionsRepo = MockPredictionsRepository(
            onConnectV2: { _ in joinExpectation.fulfill() },
            onDisconnect: { leaveExpectation.fulfill() }
        )
//        let objects = TestData.clone()
        let nearbyVM = NearbyViewModel()
        nearbyVM.alerts = .init(alerts: [:])
        nearbyVM.nearbyState = .init(loadedLocation: mockLocation, loading: false, stopIds: [])
        let sut = NearbyTransitView(
            predictionsRepository: predictionsRepo,
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ScenePhase.background)

        wait(for: [leaveExpectation], timeout: 1)

        try sut.inspect().implicitAnyView().vStack().callOnChange(newValue: ScenePhase.active)

        wait(for: [joinExpectation], timeout: 1)
    }

    @MainActor func testScrollToTopWhenNearbyChanges() throws {
        let scrollPositionSetExpectation = XCTestExpectation(description: "component scrolled")
        let now = EasternTimeInstant.now()

        let objects = TestData.clone()
        let nearbyVM = NearbyViewModel()
        nearbyVM.nearbyState = .init(loadedLocation: mockLocation, loading: false, stopIds: [])

        let route: RouteCardData.LineOrRoute = .route(TestData.getRoute(id: "67"))
        let stop = objects.getStop(id: "141")
        nearbyVM.routeCardData = [.init(lineOrRoute: route,
                                        stopData: [.init(lineOrRoute: route,
                                                         stop: stop,
                                                         data: [.init(lineOrRoute: route,
                                                                      stop: stop,
                                                                      directionId: 0,
                                                                      routePatterns: [
                                                                          TestData.getRoutePattern(id: "67-4-0"),
                                                                      ],
                                                                      stopIds: ["141"],
                                                                      upcomingTrips: [],
                                                                      alertsHere: [],
                                                                      allDataLoaded: true,
                                                                      hasSchedulesToday: true,
                                                                      alertsDownstream: [],
                                                                      context: .nearbyTransit)],
                                                         globalData: GlobalResponse(objects: objects))],
                                        at: now)]

        let sut = NearbyTransitView(
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        )

        sut.scrollSubject.sink { _ in
            scrollPositionSetExpectation.fulfill()
        }.store(in: &cancellables)

        ViewHosting.host(view: sut.withFixedSettings([:]))

        try sut.inspect().implicitAnyView().vStack()
            .callOnChange(newValue: ["new-stop"])

        wait(for: [scrollPositionSetExpectation], timeout: 2)
    }

    @MainActor func testEmptyFallback() throws {
        let nearbyVM = NearbyViewModel()
        nearbyVM.routeCardData = []
        nearbyVM.nearbyState = .init(loadedLocation: mockLocation, loading: false, stopIds: [])
        let sut = NearbyTransitView(
            predictionsRepository: MockPredictionsRepository(),
            schedulesRepository: MockScheduleRepository(),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            globalData: GlobalResponse(objects: ObjectCollectionBuilder()),
            nearbyVM: nearbyVM,
            noNearbyStops: noNearbyStops
        ).withFixedSettings([:])

        XCTAssertNil(try? sut.inspect().find(LoadingCard<Text>.self))
        XCTAssertNotNil(try sut.inspect().find(text: "No nearby stops"))
        XCTAssertNotNil(try sut.inspect().find(text: "You’re outside the MBTA service area."))
    }
}

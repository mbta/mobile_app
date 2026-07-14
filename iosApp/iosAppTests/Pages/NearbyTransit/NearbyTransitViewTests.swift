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

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testPending() throws {
        let sut = NearbyTransitView(
            alerts: .init(alerts: [:]),
            location: .constant(ViewportProvider.Defaults.center),
            setIsReturningFromBackground: { _ in },
            noNearbyStops: noNearbyStops,
            nearbyVM: MockNearbyViewModel(),
            navManager: .init(),
            viewportProvider: .init(),
        ).withFixedSettings([:])
        let cards = try sut.inspect().findAll(RouteCard.self)
        XCTAssertEqual(cards.count, 5)
        for card in cards {
            XCTAssertNotNil(try card.modifier(LoadingPlaceholderModifier.self))
        }
    }

    func testLoading() {
        let activeExpectation = expectation(description: "getNearby")
        let mockNearbyVM = MockNearbyViewModel()
        activeExpectation.assertForOverFulfill = false
        mockNearbyVM.onSetActive = { _, _ in activeExpectation.fulfill() }

        var sut = NearbyTransitView(
            alerts: .init(alerts: [:]),
            location: .constant(ViewportProvider.Defaults.center),
            setIsReturningFromBackground: { _ in },
            noNearbyStops: noNearbyStops,
            nearbyVM: mockNearbyVM,
            navManager: .init(),
            viewportProvider: .init(),
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
        wait(for: [activeExpectation], timeout: 5)
    }

    var mockLocation = CLLocationCoordinate2D(latitude: 12.34, longitude: -56.78)

    @MainActor func testWithPredictions() throws {
        NSTimeZone.default = try XCTUnwrap(TimeZone(identifier: "America/New_York"))
        let now = EasternTimeInstant.now()
        let distantMinutes = 10
        let objects = TestData.clone()

        let route: LineOrRoute = .route(TestData.getRoute(id: "67"))
        let stop = objects.getStop(id: "14121")
        let pattern = objects.getRoutePattern(id: "67-4-0")
        let trip = objects.getTrip(id: pattern.representativeTripId)
        let prediction = objects.prediction { prediction in
            prediction.arrivalTime = now.plus(minutes: Int32(distantMinutes))
            prediction.departureTime = now.plus(minutes: Int32(distantMinutes + 2))
            prediction.routeId = "67"
            prediction.stopId = "14121"
            prediction.tripId = "68596786"
        }

        let nearbyVM = MockNearbyViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            routeCardData: [
                .init(
                    lineOrRoute: route,
                    stopData: [
                        .init(
                            lineOrRoute: route,
                            stop: stop,
                            data: [
                                .init(
                                    lineOrRoute: route,
                                    stop: stop,
                                    direction: .init(
                                        directionId: 0,
                                        route: route.sortRoute
                                    ),
                                    routePatterns: [TestData.getRoutePattern(id: "67-4-0"),],
                                    stopIds: ["14121"],
                                    upcomingTrips: [.init(trip: trip, prediction: prediction)],
                                    alertsHere: [],
                                    allDataLoaded: true,
                                    hasSchedulesToday: true,
                                    subwayServiceStartTime: nil,
                                    alertsDownstream: [],
                                    context: .nearbyTransit
                                )
                            ]
                        )
                    ],
                    at: now
                )
            ],
            loadedLocation: mockLocation.positionKt,
            loadedStopIds: [stop.id]
        ))

        loadKoinMocks(objects: objects)

        let sut = NearbyTransitView(
            alerts: .init(alerts: [:]),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            noNearbyStops: noNearbyStops,
            nearbyVM: nearbyVM,
            navManager: .init(),
            viewportProvider: .init(),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        let hasAppeared = sut.inspection.inspect(after: 1) { view in
            XCTAssertNotNil(try view.find(text: "67"))
            XCTAssertNotNil(try view.find(text: "Alewife"))
            XCTAssertNotNil(try view.find(text: "10 min"))
        }
        wait(for: [hasAppeared], timeout: 2)
    }

    @MainActor func testUpdatesLocation() {
        let objects = TestData.clone()
        let davis = objects.stop { stop in
            stop.id = "place-davis"
            stop.latitude = 0.0
            stop.longitude = 0.0
        }
        let alewife = objects.stop { stop in
            stop.id = "place-alfcl"
            stop.latitude = 0.1
            stop.longitude = 0.1
        }
        let davisExp = expectation(description: "joins predictions for Davis")
        let alewifeExp = expectation(description: "joins predictions for Alewife")

        loadKoinMocks(objects: objects)

        let nearbyVM = MockNearbyViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            routeCardData: nil,
            loadedLocation: nil,
            loadedStopIds: nil,
        ))
        nearbyVM.onSetLocation = { location in
            if location == davis.position {
                davisExp.fulfill()
            }
            if location == alewife.position {
                alewifeExp.fulfill()
            }
        }

        let sut = NearbyTransitView(
            alerts: .init(alerts: [:]),
            location: .constant(davis.coordinate),
            setIsReturningFromBackground: { _ in },
            noNearbyStops: noNearbyStops,
            nearbyVM: nearbyVM,
            navManager: .init(),
            viewportProvider: .init(),
        )

        let loadExp = sut.inspection.inspect(after: 1) { view in
            try view.find(ViewType.VStack.self).callOnChange(newValue: alewife.coordinate)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [loadExp, davisExp, alewifeExp], timeout: 2)
    }

    @MainActor func testScrollToTopWhenNearbyChanges() {
        let scrollPositionSetExpectation = XCTestExpectation(description: "component scrolled")
        let now = EasternTimeInstant.now()

        let objects = TestData.clone()

        let route: LineOrRoute = .route(TestData.getRoute(id: "67"))
        let stop = objects.getStop(id: "14121")

        let nearbyState = NearbyViewModel.State(
            awaitingPredictionsAfterBackground: false,
            routeCardData: [.init(
                lineOrRoute: route,
                stopData: [.init(
                    lineOrRoute: route,
                    stop: stop,
                    data: [.init(
                        lineOrRoute: route,
                        stop: stop,
                        direction: .init(
                            directionId: 0,
                            route: route.sortRoute
                        ),
                        routePatterns: [TestData.getRoutePattern(id: "67-4-0")],
                        stopIds: ["14121"],
                        upcomingTrips: [],
                        alertsHere: [],
                        allDataLoaded: true,
                        hasSchedulesToday: true,
                        subwayServiceStartTime: nil,
                        alertsDownstream: [],
                        context: .nearbyTransit
                    )]
                )],
                at: now
            )],
            loadedLocation: mockLocation.positionKt,
            loadedStopIds: ["14121"]
        )
        let nearbyVM = MockNearbyViewModel(initialState: nearbyState)

        let sut = NearbyTransitView(
            alerts: .init(alerts: [:]),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            noNearbyStops: noNearbyStops,
            nearbyVM: nearbyVM,
            navManager: .init(),
            viewportProvider: .init(),
        )

        sut.scrollSubject.sink { _ in
            scrollPositionSetExpectation.fulfill()
        }.store(in: &cancellables)

        ViewHosting.host(view: sut.withFixedSettings([:]))

        let changeStopsExp = sut.inspection.inspect(after: 1) { view in
            try view.find(ViewType.VStack.self)
                .callOnChange(newValue: ["new-stop"])
        }

        wait(for: [changeStopsExp, scrollPositionSetExpectation], timeout: 2)
    }

    @MainActor func testEmptyFallback() {
        let nearbyVM = MockNearbyViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            routeCardData: [],
            loadedLocation: mockLocation.positionKt,
            loadedStopIds: []
        ))

        loadKoinMocks(objects: .init())

        let sut = NearbyTransitView(
            alerts: .init(alerts: [:]),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            noNearbyStops: noNearbyStops,
            nearbyVM: nearbyVM,
            navManager: .init(),
            viewportProvider: .init(),
        )

        sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNil(try? view.find(LoadingCard<Text>.self))
            XCTAssertNotNil(try view.find(text: "No nearby stops"))
            XCTAssertNotNil(try view.find(text: "You’re outside the MBTA service area."))
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
    }

    @MainActor func testFilterChangesWithAlerts() {
        let initialSetExp = expectation(description: "set empty alerts")
        let updateSetExp = expectation(description: "set populated alerts")

        let objects = TestData.clone()

        let harvardNorthbound = objects.getStop(id: "70068")
        let alert = objects.alert { alert in
            alert.effect = .suspension
            alert.informedEntity(activities: [.board, .exit, .ride], stop: harvardNorthbound.id)
        }

        let repositories = MockRepositories()
        repositories.useObjects(objects: objects)
        loadKoinMocks(repositories: repositories)

        let nearbyVM = MockNearbyViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            routeCardData: [],
            loadedLocation: mockLocation.positionKt,
            loadedStopIds: []
        ))
        nearbyVM.onSetAlerts = { alerts in
            if alerts?.getAlert(alertId: alert.id) == nil {
                initialSetExp.fulfill()
            } else {
                updateSetExp.fulfill()
            }
        }

        let sut = NearbyTransitView(
            alerts: .init(alerts: [:]),
            location: .constant(mockLocation),
            setIsReturningFromBackground: { _ in },
            noNearbyStops: noNearbyStops,
            nearbyVM: nearbyVM,
            navManager: .init(),
            viewportProvider: .init(),
        )

        let changeAlertExp = sut.inspection.inspect(after: 1) { view in
            try view.find(ViewType.VStack.self)
                .callOnChange(newValue: AlertsStreamDataResponse(alerts: [alert.id: alert]))
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))

        wait(for: [changeAlertExp, initialSetExp, updateSetExp], timeout: 3)
    }
}

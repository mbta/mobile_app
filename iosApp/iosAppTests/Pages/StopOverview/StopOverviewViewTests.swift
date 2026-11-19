//
//  StopOverviewViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/20/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class StopOverviewViewTests: XCTestCase {
    // can’t initialize these in place because can’t reference other instance variables while initializing, can’t
    // override init for XCTestCase Swift interop reasons, no equivalent to Kotlin’s lateinit, must make these optional
    // variables and then force-unwrap every time they’re used
    var builder: ObjectCollectionBuilder?
    var now: EasternTimeInstant?
    var route: Route?
    var routePatternOne: RoutePattern?
    var routePatternTwo: RoutePattern?
    var stop: Stop?
    var inaccessibleStop: Stop?
    var globalResponse: GlobalResponse?

    override func setUp() {
        executionTimeAllowance = 60

        builder = ObjectCollectionBuilder()
        let now = EasternTimeInstant.now()
        self.now = now
        route = builder!.route { route in
            route.id = "route_1"
            route.type = .bus
            route.color = "FF0000"
            route.directionNames = ["North", "South"]
            route.directionDestinations = ["Downtown", "Uptown"]
            route.longName = "Sample Route Long Name"
            route.shortName = "Sample Route"
            route.textColor = "000000"
            route.lineId = "line_1"
            route.routePatternIds = ["pattern_1", "pattern_2"]
        }
        routePatternOne = builder!.routePattern(route: route!) { routePattern in
            routePattern.id = "pattern_1"
            routePattern.directionId = 0
            routePattern.name = "Sample Route Pattern"
            routePattern.routeId = "route_1"
            routePattern.representativeTripId = "trip_1"
        }
        routePatternTwo = builder!.routePattern(route: route!) { routePattern in
            routePattern.id = "pattern_2"
            routePattern.directionId = 1
            routePattern.name = "Sample Route Pattern Two"
            routePattern.routeId = "route_1"
            routePattern.representativeTripId = "trip_1"
        }
        stop = builder!.stop { stop in
            stop.id = "stop_1"
            stop.name = "Sample Stop"
            stop.locationType = .stop
            stop.latitude = 0.0
            stop.longitude = 0.0
            stop.wheelchairBoarding = .accessible
        }
        inaccessibleStop = builder!.stop { stop in
            stop.id = "stop_2"
            stop.name = "Inaccessible Stop"
            stop.locationType = .stop
            stop.latitude = 0.0
            stop.longitude = 0.0
            stop.wheelchairBoarding = .inaccessible
        }
        builder!.line { line in
            line.id = "line_1"
            line.color = "FF0000"
            line.textColor = "FFFFFF"
        }
        builder!.trip { trip in
            trip.id = "trip_1"
            trip.routeId = "route_1"
            trip.directionId = 0
            trip.headsign = "Sample Headsign"
            trip.routePatternId = "pattern_1"
        }
        builder!.prediction { prediction in
            prediction.id = "prediction_1"
            prediction.revenue = true
            prediction.stopId = "stop_1"
            prediction.tripId = "trip_1"
            prediction.routeId = "route_1"
            prediction.stopSequence = 1
            prediction.directionId = 0
            prediction.arrivalTime = now.plus(minutes: 1)
            prediction.departureTime = now.plus(minutes: 1).plus(seconds: 30)
        }

        globalResponse = .init(
            objects: builder!,
            patternIdsByStop: [stop!.id: [routePatternOne!.id, routePatternTwo!.id]]
        )
        loadKoinMocks(objects: builder!)
    }

    private let errorBannerViewModel = MockErrorBannerViewModel()

    @MainActor func testFiltersInSameOrderAsRouteCardData() throws {
        let objects = ObjectCollectionBuilder()
        let routeDefaultSort0 = objects.route { route in
            route.sortOrder = 0
            route.type = .bus
            route.shortName = "Should be second"
        }
        let routeDefaultSort1 = objects.route { route in
            route.sortOrder = 1
            route.type = .bus
            route.shortName = "Should be first"
        }
        let stop = objects.stop { _ in }

        loadKoinMocks(objects: objects)

        let filters = StopOverviewPageFilters(stopId: stop.id)

        let sut = StopOverviewView(
            filters: filters,
            routeData: StopCentricViewModel.RouteDataStopOverview(
                filteredWith: filters,
                routeCards: [
                    .init(
                        lineOrRoute: .route(routeDefaultSort1),
                        stopData: [],
                        at: EasternTimeInstant.now()
                    ),
                    .init(
                        lineOrRoute: .route(routeDefaultSort0),
                        stopData: [],
                        at: EasternTimeInstant.now()
                    ),
                ]
            ),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: .now(),
            setRouteStopFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            navManager: .init(),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }
        let routePills = try sut.inspect().find(StopOverviewFilterPills.self).findAll(RoutePill.self)
        XCTAssertEqual(2, routePills.count)
        XCTAssertNotNil(try routePills[0].find(text: "Should be first"))
        XCTAssertNotNil(try routePills[1].find(text: "Should be second"))
    }

    func testSkipsPillsIfOneRoute() {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.shortName = "57"
        }
        let stop = objects.stop { _ in }

        let filters = StopOverviewPageFilters(stopId: stop.id)

        let sut = StopOverviewView(
            filters: filters,
            routeData: StopCentricViewModel.RouteDataStopOverview(
                filteredWith: filters,
                routeCards: [.init(
                    lineOrRoute: .route(route),
                    stopData: [.init(
                        route: route,
                        stop: stop,
                        data: []
                    )],
                    at: EasternTimeInstant.now()
                )]
            ),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: .now(),
            setRouteStopFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            navManager: .init(),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }
        XCTAssertNil(try? sut.inspect().find(StopOverviewFilterPills.self))
        XCTAssertNil(try? sut.inspect().find(button: "All"))
    }

    func testShowsElevatorAlerts() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.shortName = "Blue"
        }
        let stop = objects.stop { _ in }
        let alert = objects.alert { alert in
            alert.effect = .elevatorClosure
            alert.header = "Alert header"
        }

        let filters = StopOverviewPageFilters(stopId: stop.id)

        let sut = StopOverviewView(
            filters: filters,
            routeData: StopCentricViewModel.RouteDataStopOverview(
                filteredWith: filters,
                routeCards: [.init(
                    lineOrRoute: .route(route),
                    stopData: [.init(
                        route: route, stop: stop,
                        data: [.init(
                            lineOrRoute: .route(route),
                            stop: stop,
                            direction: .init(directionId: 0, route: route),
                            routePatterns: [],
                            stopIds: [],
                            upcomingTrips: [],
                            alertsHere: [alert],
                            allDataLoaded: true,
                            hasSchedulesToday: true,
                            subwayServiceStartTime: nil,
                            alertsDownstream: [],
                            context: .stopOverview
                        )]
                    )],
                    at: EasternTimeInstant.now()
                )]
            ),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: .now(),
            setRouteStopFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            navManager: .init(),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }
        XCTAssertNil(try? sut.inspect().find(AlertCard.self))
        XCTAssertNil(try? sut.inspect().find(text: try XCTUnwrap(alert.header)))
    }

    @MainActor func testCloseButtonCloses() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let filters = StopOverviewPageFilters(stopId: stop.id)

        var closeCalled = false
        let sut = StopOverviewView(
            filters: filters,
            routeData: nil,
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: .now(),
            setRouteStopFilter: { _ in },
            navCallbacks: .init(onBack: nil, onClose: { closeCalled = true }, backButtonPresentation: .floating),
            errorBannerVM: MockErrorBannerViewModel(),
            navManager: .init(),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }
        try sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()
        XCTAssert(closeCalled)
    }

    func testDebugModeNotShownByDefault() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.id = "FAKE_STOP_ID"
        }

        let navManager = NavigationManager(navigationStack: [.stopOverview(stopId: stop.id)])

        let filters = StopOverviewPageFilters(stopId: stop.id)

        let sut = StopOverviewView(
            filters: filters,
            routeData: nil,
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: .now(),
            setRouteStopFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            navManager: .init(),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }
        XCTAssertThrowsError(try sut.inspect().find(text: "stop id: FAKE_STOP_ID"))
    }

    func testDebugModeShown() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { stop in
            stop.id = "FAKE_STOP_ID"
        }

        let navManager = NavigationManager(
            navigationStack: [.stopOverview(stopId: stop.id)]
        )

        let filters = StopOverviewPageFilters(stopId: stop.id)

        let sut = StopOverviewView(
            filters: filters,
            routeData: nil,
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: .now(),
            setRouteStopFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            navManager: .init(),
        )

        ViewHosting.host(view: sut.withFixedSettings([.devDebugMode: true]))
        defer { ViewHosting.expel() }
        try sut.inspect().findAll(ViewType.Text.self).forEach { view in try print(view.string()) }
        XCTAssertNotNil(try sut.inspect().find(text: "stop id: FAKE_STOP_ID"))
    }

    @MainActor
    func testGroupsByDirection() async throws {
        let routeCardData = try await RouteCardData.companion.routeCardsForStopList(
            stopIds: [XCTUnwrap(stop?.id)] + stop!.childStopIds,
            globalData: XCTUnwrap(globalResponse),
            sortByDistanceFrom: nil,
            schedules: nil,
            predictions: .init(objects: XCTUnwrap(builder)),
            alerts: .init(alerts: [:]),
            now: XCTUnwrap(now),
            context: .stopOverview
        )!

        let sut = try StopOverviewView(
            filters: .init(stopId: XCTUnwrap(stop?.id)),
            routeData: StopCentricViewModel.RouteDataStopOverview(
                filteredWith: .init(stopId: XCTUnwrap(stop?.id)),
                routeCards: routeCardData
            ),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: XCTUnwrap(builder)),
            now: XCTUnwrap(now),
            setRouteStopFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: errorBannerViewModel,
            navManager: .init(),
        )

        let exp = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(text: "Sample Route"))
            XCTAssertNotNil(try view.find(text: "Sample Headsign"))
            XCTAssertNotNil(try view.find(text: "1 min"))
            XCTAssertThrowsError(try view.find(text: "This stop is not accessible"))
        }

        try await ViewHosting.host(sut.withFixedSettings([:])) {
            await fulfillment(of: [exp], timeout: 2)
        }
    }

    @MainActor func testInaccessibleByDirection() async throws {
        let routeCardData = try await RouteCardData.companion.routeCardsForStopList(
            stopIds: [XCTUnwrap(inaccessibleStop?.id)] + inaccessibleStop!.childStopIds,
            globalData: XCTUnwrap(globalResponse),
            sortByDistanceFrom: nil,
            schedules: nil,
            predictions: .init(objects: XCTUnwrap(builder)),
            alerts: .init(alerts: [:]),
            now: XCTUnwrap(now),
            context: .stopOverview
        )!

        let sut = try StopOverviewView(
            filters: .init(stopId: XCTUnwrap(inaccessibleStop?.id)),
            routeData: StopCentricViewModel.RouteDataStopOverview(
                filteredWith: .init(stopId: XCTUnwrap(inaccessibleStop?.id)),
                routeCards: routeCardData
            ),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: XCTUnwrap(builder)),
            now: XCTUnwrap(now),
            setRouteStopFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: errorBannerViewModel,
            navManager: .init(),
        )

        let exp = sut.inspection.inspect(after: 0.5) { view in
            XCTAssertNotNil(try view.find(text: "This stop is not accessible"))
        }
        try await ViewHosting.host(sut.withFixedSettings([.stationAccessibility: true])) {
            await fulfillment(of: [exp], timeout: 2)
        }
    }

    @MainActor func testShowsElevatorAlertsWhenGroupedByDirection() async throws {
        let alert = try XCTUnwrap(builder?.clone().alert { alert in
            alert.header = "Elevator alert"
            alert.effect = .elevatorClosure
            alert.informedEntity(
                activities: [.usingWheelchair],
                directionId: nil,
                facility: nil,
                route: nil,
                routeType: nil,
                stop: self.stop!.id,
                trip: nil
            )
        })

        let routeCardData = try await RouteCardData.companion.routeCardsForStopList(
            stopIds: [XCTUnwrap(stop?.id)] + stop!.childStopIds,
            globalData: XCTUnwrap(globalResponse),
            sortByDistanceFrom: nil,
            schedules: nil,
            predictions: .init(objects: XCTUnwrap(builder)),
            alerts: .init(alerts: [alert.id: alert]),
            now: XCTUnwrap(now),
            context: .stopOverview
        )!

        let overviewView = try StopOverviewView(
            filters: .init(stopId: XCTUnwrap(stop?.id)),
            routeData: StopCentricViewModel.RouteDataStopOverview(
                filteredWith: .init(stopId: XCTUnwrap(stop?.id)),
                routeCards: routeCardData
            ),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: XCTUnwrap(builder)),
            now: XCTUnwrap(now),
            setRouteStopFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: errorBannerViewModel,
            navManager: .init(),
        )

        let sut = overviewView.withFixedSettings([.stationAccessibility: true])
        XCTAssertNotNil(try sut.inspect().find(text: "Elevator alert"))
    }
}

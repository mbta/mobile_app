//
//  RouteStopViewTests.swift
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

final class RouteStopViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
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

        let filters = RouteStopPageFilters(
            stopId: stop.id,
            routeStopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil
        )

        let sut = RouteStopView(
            filters: filters,
            routeData: StopCentricViewModel.RouteDataRouteStop(
                filteredWith: filters,
                stopData: .init(
                    lineOrRoute: .route(route),
                    stop: stop,
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
                )
            ),
            alerts: .init(alerts: [alert.id: alert]),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setRouteStopFilter: { _ in },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopCentricVM: MockStopCentricViewModel(),
            navManager: .init(),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }
        XCTAssertNil(try? sut.inspect().find(AlertCard.self))
        XCTAssertNil(try? sut.inspect().find(text: try XCTUnwrap(alert.header)))
    }

    func testDisplaysVehicleData() {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.shortName = "57"
        }
        let stop = objects.stop { _ in }
        let routePattern = objects.routePattern(route: route) { _ in }
        let trip = objects.trip(routePattern: routePattern) { _ in }
        let prediction = objects.prediction { prediction in
            prediction.departureTime = now.plus(seconds: 100)
        }
        let vehicle = objects.vehicle { vehicle in
            vehicle.tripId = trip.id
            vehicle.currentStatus = .inTransitTo
        }

        let leaf = RouteCardData.Leaf(
            lineOrRoute: .route(route),
            stop: stop,
            direction: .init(directionId: 0, route: route),
            routePatterns: [routePattern],
            stopIds: Set([stop.id]),
            upcomingTrips: [.init(trip: trip, prediction: prediction)],
            alertsHere: [], allDataLoaded: true,
            hasSchedulesToday: true, subwayServiceStartTime: nil, alertsDownstream: [],
            context: .stopOverview
        )
        let stopData = RouteCardData.RouteStopData(
            lineOrRoute: .route(route),
            stop: stop,
            data: [leaf]
        )

        let filters = RouteStopPageFilters(
            stopId: stop.id,
            routeStopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: .init(tripId: trip.id, vehicleId: vehicle.id, stopSequence: 1, selectionLock: false)
        )

        let sut = RouteStopView(
            filters: filters,
            routeData: StopCentricViewModel.RouteDataRouteStop(
                filteredWith: filters,
                stopData: stopData
            ),
            alerts: .init(alerts: [:]),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setRouteStopFilter: { _ in },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopCentricVM: MockStopCentricViewModel(),
            navManager: .init(),
        )

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([:]))
        defer { ViewHosting.expel() }
        XCTAssertNotNil(try? sut.inspect().find(TripDetailsView.self))
    }

    @MainActor func testCloseButtonCloses() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { _ in }

        let filters = RouteStopPageFilters(
            stopId: stop.id,
            routeStopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil
        )

        var closeCalled = false
        let sut = RouteStopView(
            filters: filters,
            routeData: nil,
            alerts: .init(alerts: [:]),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setRouteStopFilter: { _ in },
            setTripFilter: { _ in },
            navCallbacks: .init(onBack: nil, onClose: { closeCalled = true }, backButtonPresentation: .floating),
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopCentricVM: MockStopCentricViewModel(),
            navManager: .init(),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }
        try sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()
        XCTAssert(closeCalled)
    }

    func testDebugModeNotShownByDefault() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { stop in
            stop.id = "FAKE_STOP_ID"
        }

        let navManager = NavigationManager(navigationStack: [.stopOverview(stopId: stop.id)])

        let filters = RouteStopPageFilters(
            stopId: stop.id,
            routeStopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil
        )

        let sut = RouteStopView(
            filters: filters,
            routeData: nil,
            alerts: .init(alerts: [:]),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setRouteStopFilter: { _ in },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopCentricVM: MockStopCentricViewModel(),
            navManager: .init(),
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }
        XCTAssertThrowsError(try sut.inspect().find(text: "stop id: FAKE_STOP_ID"))
    }

    func testDebugModeShown() throws {
        let objects = ObjectCollectionBuilder()
        let route = objects.route()
        let stop = objects.stop { stop in
            stop.id = "FAKE_STOP_ID"
        }

        let navManager = NavigationManager(
            navigationStack: [.stopOverview(stopId: stop.id)]
        )

        let filters = RouteStopPageFilters(
            stopId: stop.id,
            routeStopFilter: .init(routeId: route.id, directionId: 0),
            tripFilter: nil
        )

        let sut = RouteStopView(
            filters: filters,
            routeData: nil,
            alerts: .init(alerts: [:]),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setRouteStopFilter: { _ in },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopCentricVM: MockStopCentricViewModel(),
            navManager: .init(),
        )

        ViewHosting.host(view: sut.withFixedSettings([.devDebugMode: true]))
        defer { ViewHosting.expel() }
        try sut.inspect().findAll(ViewType.Text.self).forEach { view in try print(view.string()) }
        XCTAssertNotNil(try sut.inspect().find(text: "stop id: FAKE_STOP_ID"))
    }

    @MainActor func testSaveEnhancedFavoriteTriggersSaveFlow() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()
        let directionId: Int32 = 0

        let favoritesRepository = MockFavoritesRepository()

        let sut = RouteStopView(
            filters: .init(
                stopId: stop.id,
                routeStopFilter: .init(routeId: route.id, directionId: directionId),
                tripFilter: nil
            ),
            routeData: nil,
            alerts: .init(alerts: [:]),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setRouteStopFilter: { _ in },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopCentricVM: MockStopCentricViewModel(),
            navManager: .init()
        )

        let tappedPublisher = PassthroughSubject<Void, Never>()

        let tapButtonExp = sut.inspection.inspect(after: 0.5) { view in
            try view.find(StarButton.self).find(ViewType.Button.self)
                .tap()
            tappedPublisher.send()
        }

        try? sut.inspect().find(button: "Save").tap()

        let starButtonExp = sut.inspection.inspect(after: 1) { view in
            let button = try view.find(StarButton.self).find(ViewType.Button.self)

            XCTAssertFalse(button.isDisabled())
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([
            .devDebugMode: false,
        ]))

        defer { ViewHosting.expel() }
        wait(for: [tapButtonExp, starButtonExp], timeout: 1)
    }

    @MainActor func testShowsDataWhenStopAndRouteMatchFilterButDirectionDoesnt() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()
        let directionId: Int32 = 0
        let now = EasternTimeInstant.now()

        let pattern0 = objects.routePattern(route: route) { pattern in
            pattern.routeId = route.id.idText
            pattern.directionId = directionId
            pattern.representativeTripId = "trip0"
        }

        let trip0 = objects.trip { trip in
            trip.id = pattern0.representativeTripId
            trip.headsign = "Alewife"
            trip.routeId = route.id.idText
            trip.routePatternId = pattern0.id
            trip.stopIds = [stop.id]
        }

        let upcomingTrip0 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = trip0
            prediction.departureTime = now.plus(minutes: 2)
        })

        let pattern1 = objects.routePattern(route: route) { pattern in
            pattern.routeId = route.id.idText
            pattern.directionId = 1
            pattern.representativeTripId = "trip1"
        }

        let trip1 = objects.trip { trip in
            trip.id = pattern1.representativeTripId
            trip.headsign = "Broadway"
            trip.routeId = route.id.idText
            trip.routePatternId = pattern1.id
            trip.stopIds = [stop.id]
        }

        let upcomingTrip1 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = trip1
            prediction.departureTime = now.plus(minutes: 4)
        })

        let favoritesRepository = MockFavoritesRepository()

        let stopData: RouteCardData.RouteStopData = .init(route: route, stop: stop, data: [RouteCardData.Leaf(
            lineOrRoute: LineOrRoute.Route(route: route),
            stop: stop,
            // key piece: directionId here is 0
            direction: .init(directionId: 0, route: route),
            routePatterns: [],
            stopIds: Set([stop.id]),
            upcomingTrips: [upcomingTrip0],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            subwayServiceStartTime: nil,
            alertsDownstream: [],
            context: .routeStop
        ), RouteCardData.Leaf(
            lineOrRoute: LineOrRoute.Route(route: route),
            stop: stop,
            direction: .init(directionId: 1, route: route),
            routePatterns: [],
            stopIds: Set([stop.id]),
            upcomingTrips: [upcomingTrip1],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            subwayServiceStartTime: nil,
            alertsDownstream: [],
            context: .routeStop
        )])

        let routeData = StopCentricViewModel.RouteDataRouteStop(
            filteredWith: .init(stopId: stop.id, routeStopFilter: .init(routeId: route.id, directionId: 1),
                                tripFilter: nil),
            stopData: stopData
        )

        let stopCentricVM = MockStopCentricViewModel(initialState: .init(routeData: routeData,
                                                                         awaitingPredictionsAfterBackground: false))

        let sut = RouteStopView(
            filters: .init(
                stopId: stop.id,
                // key piece: directionId here is 1
                routeStopFilter: .init(routeId: route.id, directionId: 1),
                tripFilter: nil
            ),
            routeData: routeData,
            alerts: .init(alerts: [:]),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setRouteStopFilter: { _ in },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopCentricVM: stopCentricVM,
            navManager: .init()
        )

        let exp = sut.inspection.inspect(after: 2) { view in
            try view.find(text: "4 min")
        }
        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([
            .devDebugMode: false,
        ]))
        defer { ViewHosting.expel() }
        wait(for: [exp], timeout: 4)
    }

    @MainActor func testCanSeeDirectionLabelsBeforeRealtimeDataLoads() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let otherStop = objects.stop { _ in }

        let route = objects.route { route in
            route.directionNames = ["Outbound", "Inbound"]
            route.directionDestinations = ["Here", "There"]
        }
        let directionId: Int32 = 0
        let now = EasternTimeInstant.now()

        let pattern0 = objects.routePattern(route: route) { pattern in
            pattern.routeId = route.id.idText
            pattern.directionId = directionId
            pattern.representativeTripId = "trip0"
            pattern.typicality = .typical
        }

        let trip0 = objects.trip { trip in
            trip.id = pattern0.representativeTripId
            trip.headsign = "Alewife"
            trip.routeId = route.id.idText
            trip.routePatternId = pattern0.id
            trip.stopIds = [stop.id, otherStop.id]
        }

        let upcomingTrip0 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = trip0
            prediction.departureTime = now.plus(minutes: 2)
        })

        let pattern1 = objects.routePattern(route: route) { pattern in
            pattern.routeId = route.id.idText
            pattern.directionId = 1
            pattern.representativeTripId = "trip1"
        }

        let trip1 = objects.trip { trip in
            trip.id = pattern1.representativeTripId
            trip.headsign = "Broadway"
            trip.routeId = route.id.idText
            trip.routePatternId = pattern1.id
            trip.stopIds = [stop.id]
        }

        let upcomingTrip1 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = trip1
            prediction.departureTime = now.plus(minutes: 4)
        })

        let favoritesRepository = MockFavoritesRepository()

        let stopCentricVM = MockStopCentricViewModel(initialState: .init(routeData: nil,
                                                                         awaitingPredictionsAfterBackground: false))

        let sut = RouteStopView(
            filters: .init(
                stopId: stop.id,
                routeStopFilter: .init(routeId: route.id, directionId: directionId),
                tripFilter: nil
            ),
            routeData: nil,
            alerts: .init(alerts: [:]),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setRouteStopFilter: { _ in },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopCentricVM: stopCentricVM,
            navManager: .init()
        )

        let exp = sut.inspection.inspect(after: 2) { view in
            XCTAssertNotNil(try view.find(text: "Outbound to"))
        }
        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([
            .devDebugMode: false,
        ]))
        defer { ViewHosting.expel() }
        wait(for: [exp], timeout: 4)
    }

    @MainActor func testCorrectlySetsStopFilterWhenChangingDirectionAtGLStop() throws {
        let objects = TestData.clone()

        let route: Route = objects.getRoute(id: "Green-B")
        let stop = objects.getStop(id: "place-boyls")

        let directionId: Int32 = 0
        let now = EasternTimeInstant.now()

        let pattern0 = objects.getRoutePattern(id: "Green-B-812-0")
        let pattern1 = objects.getRoutePattern(id: "Green-B-812-1")

        let trip0 = objects.getTrip(id: "canonical-Green-B-C1-0")
        let trip1 = objects.getTrip(id: "canonical-Green-B-C1-0")

        let upcomingTrip0 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = trip0
            prediction.departureTime = now.plus(minutes: 2)
        })

        let upcomingTrip1 = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = trip1
            prediction.departureTime = now.plus(minutes: 4)
        })

        let favoritesRepository = MockFavoritesRepository()

        let stopData: RouteCardData.RouteStopData = .init(route: route, stop: stop, data: [RouteCardData.Leaf(
            lineOrRoute: LineOrRoute.Route(route: route),
            stop: stop,
            direction: .init(directionId: 0, route: route),
            routePatterns: [],
            stopIds: Set([stop.id]),
            upcomingTrips: [upcomingTrip0],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            subwayServiceStartTime: nil,
            alertsDownstream: [],
            context: .routeStop
        ), RouteCardData.Leaf(
            lineOrRoute: LineOrRoute.Route(route: route),
            stop: stop,
            direction: .init(directionId: 1, route: route),
            routePatterns: [],
            stopIds: Set([stop.id]),
            upcomingTrips: [upcomingTrip1],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            subwayServiceStartTime: nil,
            alertsDownstream: [],
            context: .routeStop
        )])

        let routeData = try StopCentricViewModel.RouteDataRouteStop(
            filteredWith: .init(
                stopId: stop.id,
                routeStopFilter: .init(routeId: XCTUnwrap(route.lineId), directionId: 0),
                tripFilter: nil
            ),
            stopData: stopData
        )

        let stopCentricVM = MockStopCentricViewModel(initialState: .init(routeData: routeData,
                                                                         awaitingPredictionsAfterBackground: false))

        let setRouteStopFilterExp = XCTestExpectation(description: "setRouteStopFilter called for GL direction 1")

        let sut = try RouteStopView(
            filters: .init(
                stopId: stop.id,
                routeStopFilter: .init(routeId: XCTUnwrap(route.lineId), directionId: directionId),
                tripFilter: nil
            ),
            routeData: nil,
            alerts: .init(alerts: [:]),
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setRouteStopFilter: {
                filter in if filter.directionId == 1, filter.routeId.idText == "line-Green" {
                    setRouteStopFilterExp.fulfill()
                } else {
                    XCTFail("setRouteStopFilter called with wrong params \(filter.directionId) \(filter.routeId)")
                }
            },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            mapVM: MockMapViewModel(),
            stopCentricVM: stopCentricVM,
            navManager: .init()
        )

        let exp = sut.inspection.inspect(after: 2) { view in
            try view.find(button: "Eastbound to").tap()
        }
        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([
            .devDebugMode: false,
        ]))
        defer { ViewHosting.expel() }
        wait(for: [exp, setRouteStopFilterExp], timeout: 4)
    }
}

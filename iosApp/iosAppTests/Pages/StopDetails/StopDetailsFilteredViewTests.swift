//
//  StopDetailsFilteredViewTests.swift
//  iosAppTests
//
//  Created by Kayla Brady on 7/2/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsFilteredViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testSaveEnhancedFavoriteTriggersSaveFlow() {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()
        let directionId: Int32 = 0

        let favoritesRepository = MockFavoritesRepository()

        let sut = StopDetailsFilteredView(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: directionId),
            tripFilter: nil,
            routeData: nil,
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: .init(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: MockStopDetailsViewModel(),
        )

        let tappedPublisher = PassthroughSubject<Void, Never>()

        let tapButtonExp = sut.inspection.inspect(after: 0.5) { view in
            try view.find(StarButton.self).find(ViewType.Button.self)
                .tap()
            tappedPublisher.send()
        }
        let confirmationDialogExp = sut.inspection.inspect(onReceive: tappedPublisher, after: 1) { view in
            XCTAssertTrue(try view.actualView().inSaveFavoritesFlow)
        }

        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([
            .devDebugMode: false,
        ]))
        wait(for: [tapButtonExp, confirmationDialogExp], timeout: 4)
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
            directionId: 0,
            routePatterns: [],
            stopIds: Set([stop.id]),
            upcomingTrips: [upcomingTrip0],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            subwayServiceStartTime: nil,
            alertsDownstream: [],
            context: .stopDetailsFiltered
        ), RouteCardData.Leaf(
            lineOrRoute: LineOrRoute.Route(route: route),
            stop: stop,
            directionId: 1,
            routePatterns: [],
            stopIds: Set([stop.id]),
            upcomingTrips: [upcomingTrip1],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            subwayServiceStartTime: nil,
            alertsDownstream: [],
            context: .stopDetailsFiltered
        )],
        globalData: GlobalResponse(objects: objects))

        let routeData = StopDetailsViewModel.RouteDataFiltered(
            filteredWith: .init(stopId: stop.id, stopFilter: .init(routeId: route.id, directionId: 1),
                                tripFilter: nil),
            stopData: stopData
        )

        let stopDetailsVM = MockStopDetailsViewModel(initialState: .init(routeData: routeData,
                                                                         alertSummaries: [:],
                                                                         awaitingPredictionsAfterBackground: false))

        let sut = StopDetailsFilteredView(
            stopId: stop.id,
            // key piece: directionId here is 1
            stopFilter: .init(routeId: route.id, directionId: 1),
            tripFilter: nil,
            routeData: routeData,
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: .init(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: stopDetailsVM,
        )

        let exp = sut.inspection.inspect(after: 2) { view in
            try view.find(text: "4 min")
        }
        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([
            .devDebugMode: false,
        ]))
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

        let stopDetailsVM = MockStopDetailsViewModel(initialState: .init(routeData: nil,
                                                                         alertSummaries: [:],
                                                                         awaitingPredictionsAfterBackground: false))

        let sut = StopDetailsFilteredView(
            stopId: stop.id,
            stopFilter: .init(routeId: route.id, directionId: directionId),
            tripFilter: nil,
            routeData: nil,
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setStopFilter: { _ in },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: .init(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: stopDetailsVM,
        )

        let exp = sut.inspection.inspect(after: 2) { view in
            XCTAssertNotNil(try view.find(text: "Outbound to"))
        }
        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([
            .devDebugMode: false,
        ]))
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
            directionId: 0,
            routePatterns: [],
            stopIds: Set([stop.id]),
            upcomingTrips: [upcomingTrip0],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            subwayServiceStartTime: nil,
            alertsDownstream: [],
            context: .stopDetailsFiltered
        ), RouteCardData.Leaf(
            lineOrRoute: LineOrRoute.Route(route: route),
            stop: stop,
            directionId: 1,
            routePatterns: [],
            stopIds: Set([stop.id]),
            upcomingTrips: [upcomingTrip1],
            alertsHere: [],
            allDataLoaded: true,
            hasSchedulesToday: true,
            subwayServiceStartTime: nil,
            alertsDownstream: [],
            context: .stopDetailsFiltered
        )],
        globalData: GlobalResponse(objects: objects))

        let routeData = try StopDetailsViewModel.RouteDataFiltered(
            filteredWith: .init(
                stopId: stop.id,
                stopFilter: .init(routeId: XCTUnwrap(route.lineId), directionId: 0),
                tripFilter: nil
            ),
            stopData: stopData
        )

        let stopDetailsVM = MockStopDetailsViewModel(initialState: .init(routeData: routeData,
                                                                         alertSummaries: [:],
                                                                         awaitingPredictionsAfterBackground: false))

        let setStopFilterExp = XCTestExpectation(description: "setStopFilter called for GL direction 1")

        let sut = try StopDetailsFilteredView(
            stopId: stop.id,
            stopFilter: .init(routeId: XCTUnwrap(route.lineId), directionId: directionId),
            tripFilter: nil,
            routeData: nil,
            favorites: .init(routeStopDirection: [:]),
            global: .init(objects: objects),
            now: Date.now,
            onUpdateFavorites: {},
            setStopFilter: { filter in if filter?.directionId == 1, filter?.routeId.idText == "line-Green" {
                setStopFilterExp.fulfill()
            } else {
                XCTFail("setStopFilter called with wrong params \(filter?.directionId) \(filter?.routeId)")
            }
            },
            setTripFilter: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: .init(),
            mapVM: MockMapViewModel(),
            stopDetailsVM: stopDetailsVM,
        )

        let exp = sut.inspection.inspect(after: 2) { view in
            try view.find(button: "Eastbound to").tap()
        }
        ViewHosting.host(view: sut.environmentObject(ViewportProvider()).withFixedSettings([
            .devDebugMode: false,
        ]))
        wait(for: [exp, setStopFilterExp], timeout: 4)
    }
}

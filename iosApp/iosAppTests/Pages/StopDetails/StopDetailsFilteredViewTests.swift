//
//  StopDetailsFilteredViewTests.swift
//  iosAppTests
//
//  Created by Kayla Brady on 7/2/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp

import Combine
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class StopDetailsFilteredViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testSaveEnhancedFavoriteTriggersSaveFlow() throws {
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

    @MainActor func testShowsDataWhenStopAndRouteHaventChanged() throws {
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }
        let route = objects.route()
        let directionId: Int32 = 0

        let favoritesRepository = MockFavoritesRepository()

        let stopDetailsVM = MockStopDetailsViewModel(initialState: .init(
            routeData: .some(StopDetailsViewModel.RouteDataFiltered(
                filteredWith: .init(stopId: stop.id, stopFilter: .init(routeId: route.id, directionId: 1),
                                    tripFilter: nil),
                stopData: .init(route: route, stop: stop, data: [RouteCardData.Leaf(
                    lineOrRoute: LineOrRoute.Route(route: route),
                    stop: stop,
                    directionId: 0,
                    routePatterns: [],
                    stopIds: Set([stop.id]),
                    upcomingTrips: [],
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
                    upcomingTrips: [],
                    alertsHere: [],
                    allDataLoaded: true,
                    hasSchedulesToday: true,
                    subwayServiceStartTime: nil,
                    alertsDownstream: [],
                    context: .stopDetailsFiltered
                )],
                globalData: GlobalResponse(objects: objects))
            )),
            alertSummaries: [:],
            awaitingPredictionsAfterBackground: false
        ))

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

        let exp = sut.inspection.inspect(after: 1) { view in
            XCTAssertNotNil(
                try? view.find(StopDetailsFilteredPickerView.self)
            )
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }
}

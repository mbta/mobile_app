//
//  FavoritesViewTests.swift
//  iosAppTests
//
//  Created by Melody Horn on 6/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreLocation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class FavoritesViewTests: XCTestCase {
    @MainActor func testShowsFavorites() {
        let now = Date.now
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.longName = "Some Route"
        }
        let stop = objects.stop { stop in
            stop.name = "Some Stop"
        }
        let routePattern = objects.routePattern(route: route) { _ in }
        let trip = objects.upcomingTrip(prediction: objects.prediction { prediction in
            prediction.trip = objects.trip(routePattern: routePattern)
            prediction.stopId = stop.id
            prediction.departureTime = (now + 5 * 60).toKotlinInstant()
        })
        let globalData = GlobalResponse(objects: objects)
        let favoritesVM = MockFavoritesViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            favorites: [.init(route: route.id, stop: stop.id, direction: 0)],
            routeCardData: [.init(
                lineOrRoute: .route(route),
                stopData: [.init(
                    route: route,
                    stop: stop,
                    data: [.init(
                        lineOrRoute: .route(route),
                        stop: stop,
                        directionId: 0,
                        routePatterns: [routePattern],
                        stopIds: [stop.id],
                        upcomingTrips: [trip],
                        alertsHere: [],
                        allDataLoaded: true,
                        hasSchedulesToday: true,
                        alertsDownstream: [],
                        context: .favorites
                    )],
                    globalData: globalData
                )],
                at: now.toKotlinInstant()
            )],
            staticRouteCardData: [.init(
                lineOrRoute: .route(route),
                stopData: [.init(
                    route: route,
                    stop: stop,
                    data: [.init(
                        lineOrRoute: .route(route),
                        stop: stop,
                        directionId: 0,
                        routePatterns: [routePattern],
                        stopIds: [stop.id],
                        upcomingTrips: [],
                        alertsHere: [],
                        allDataLoaded: true,
                        hasSchedulesToday: false,
                        alertsDownstream: [],
                        context: .favorites
                    )],
                    globalData: globalData
                )],
                at: now.toKotlinInstant()
            )]
        ))
        let sut = FavoritesView(
            errorBannerVM: .init(),
            favoritesVM: favoritesVM,
            nearbyVM: .init(),
            location: .constant(.init(latitude: 0, longitude: 0))
        )
        let exp = sut.inspection.inspect(after: 0.2) { view in
            XCTAssertNotNil(try view.find(text: "Some Route"))
            XCTAssertNotNil(try view.find(text: "Some Stop"))
            XCTAssertNotNil(try view.find(text: "5 min"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    @MainActor func testShowsEmpty() {
        let favoritesVM = MockFavoritesViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            favorites: [],
            routeCardData: [],
            staticRouteCardData: []
        ))
        let sut = FavoritesView(
            errorBannerVM: .init(),
            favoritesVM: favoritesVM,
            nearbyVM: .init(),
            location: .constant(.init(latitude: 0, longitude: 0))
        )
        let exp = sut.inspection.inspect(after: 0.2) { view in
            XCTAssertNotNil(try view.find(text: "No stops added"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    @MainActor func testShowsLoading() {
        let favoritesVM = MockFavoritesViewModel()
        let sut = FavoritesView(
            errorBannerVM: .init(),
            favoritesVM: favoritesVM,
            nearbyVM: .init(),
            location: .constant(.init(latitude: 0, longitude: 0))
        )
        let exp = sut.inspection.inspect(after: 0.2) { view in
            XCTAssertEqual(5, view.findAll(LoadingRouteCard.self).count)
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    func testFiresReloadFavorites() throws {
        let exp = expectation(description: "calls reloadFavorites in onAppear")
        let favoritesVM = MockFavoritesViewModel()
        favoritesVM.onReloadFavorites = { exp.fulfill() }
        let sut = FavoritesView(
            errorBannerVM: .init(),
            favoritesVM: favoritesVM,
            nearbyVM: .init(),
            location: .constant(.init(latitude: 0, longitude: 0))
        ).withFixedSettings([:])
        try sut.inspect().view(FavoritesView.self).vStack().callOnAppear()
        wait(for: [exp], timeout: 1)
    }

    func testSetsActive() throws {
        let expActive = expectation(description: "sets to active")
        let expInactive = expectation(description: "sets to inactive")
        let expBackground = expectation(description: "sets to background")
        let favoritesVM = MockFavoritesViewModel()
        favoritesVM.onSetActive = { active, wasSentToBackground in
            if active.boolValue {
                XCTAssertFalse(wasSentToBackground.boolValue)
                expActive.fulfill()
            } else if !wasSentToBackground.boolValue {
                expInactive.fulfill()
            } else {
                expBackground.fulfill()
            }
        }
        let sut = FavoritesView(
            errorBannerVM: .init(),
            favoritesVM: favoritesVM,
            nearbyVM: .init(),
            location: .constant(.init(latitude: 0, longitude: 0))
        ).withFixedSettings([:])

        try sut.inspect().findAndCallOnChange(newValue: ScenePhase.inactive)
        wait(for: [expInactive], timeout: 1)
        try sut.inspect().findAndCallOnChange(newValue: ScenePhase.background)
        wait(for: [expBackground], timeout: 1)
        try sut.inspect().findAndCallOnChange(newValue: ScenePhase.active)
        wait(for: [expActive], timeout: 1)
    }

    @MainActor func testSetsAlerts() throws {
        let objects = ObjectCollectionBuilder()
        objects.alert { _ in }
        let alertsResponse = AlertsStreamDataResponse(objects: objects)
        let setAlertExp = expectation(description: "sets alerts")
        let favoritesVM = MockFavoritesViewModel()
        favoritesVM.onSetAlerts = { alerts in
            if let alerts {
                XCTAssertEqual(alertsResponse, alerts)
                setAlertExp.fulfill()
            }
        }
        let nearbyVM = NearbyViewModel()
        let sut = FavoritesView(
            errorBannerVM: .init(),
            favoritesVM: favoritesVM,
            nearbyVM: nearbyVM,
            location: .constant(.init(latitude: 0, longitude: 0))
        )
        let appearExp = sut.inspection.inspect(after: 0.2) { _ in
            nearbyVM.alerts = alertsResponse
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [appearExp, setAlertExp], timeout: 1)
    }

    @MainActor func testSetsLocation() throws {
        let firstLocation = CLLocationCoordinate2D(latitude: 0, longitude: 0)
        let secondLocation = CLLocationCoordinate2D(latitude: 1, longitude: 1)
        let locationBinding = Binding<CLLocationCoordinate2D?>(wrappedValue: firstLocation)
        let setFirstExp = expectation(description: "sets first location")
        let setSecondExp = expectation(description: "sets second location")
        let favoritesVM = MockFavoritesViewModel()
        favoritesVM.onSetLocation = { newLocation in
            if newLocation == firstLocation.positionKt {
                setFirstExp.fulfill()
            } else if newLocation == secondLocation.positionKt {
                setSecondExp.fulfill()
            } else {
                XCTFail("set location to unexpected \(newLocation.debugDescription)")
            }
        }
        let sut = FavoritesView(
            errorBannerVM: .init(),
            favoritesVM: favoritesVM,
            nearbyVM: .init(),
            location: locationBinding
        )
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [setFirstExp], timeout: 1)
        locationBinding.wrappedValue = secondLocation
        wait(for: [setSecondExp], timeout: 1)
    }

    @MainActor func testSetsNow() throws {
        let setFirstExp = expectation(description: "sets a time")
        var firstTime: KotlinInstant?
        let setSecondExp = expectation(description: "sets a different time later")
        setSecondExp.assertForOverFulfill = false
        let favoritesVM = MockFavoritesViewModel()
        favoritesVM.onSetNow = { newNow in
            if firstTime == nil {
                firstTime = newNow
                setFirstExp.fulfill()
            } else {
                XCTAssertNotEqual(firstTime, newNow)
                setSecondExp.fulfill()
            }
        }
        let sut = FavoritesView(
            errorBannerVM: .init(),
            favoritesVM: favoritesVM,
            nearbyVM: .init(),
            location: .constant(.init(latitude: 0, longitude: 0))
        )
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [setFirstExp], timeout: 1)
        wait(for: [setSecondExp], timeout: 10)
    }
}

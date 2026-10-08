//
//  EditFavoritesPageTests.swift
//  iosApp
//
//  Created by Kayla Brady on 7/11/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class EditFavoritesPageTests: XCTestCase {
    @MainActor func testHeader() throws {
        let objects = ObjectCollectionBuilder()
        let favoritesVM = MockFavoritesViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            favorites: [:],
            shouldShowFirstTimeToast: false,
            shouldShowNotificationsHint: false,
            routeCardData: [],
            stopCardData: [],
            staticRouteCardData: [],
            staticStopCardData: [],
            loadedLocation: nil
        ))

        var onCloseCalled = false
        let sut = EditFavoritesPage(
            viewModel: favoritesVM,
            navCallbacks: .init(onBack: nil, onClose: { onCloseCalled = true }, backButtonPresentation: .floating),
            onOpenEditModal: { _ in },
            errorBannerVM: MockErrorBannerViewModel(),
            toastVM: MockToastViewModel()
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))
        defer { ViewHosting.expel() }

        XCTAssertNotNil(try sut.inspect().find(text: "Edit Favorites"))
        try sut.inspect().find(button: "Done").tap()
        XCTAssertTrue(onCloseCalled)
    }

    @MainActor func testOpenEdit() throws {
        let objects = TestData.clone()

        let route: Route = objects.getRoute(id: "Red")
        let stop = objects.getStop(id: "place-asmnl")

        let expectedRsd = RouteStopDirection(route: route.id, stop: stop.id, direction: 1)
        let favoritesVM = MockFavoritesViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            favorites: [expectedRsd: .init()],
            shouldShowFirstTimeToast: false,
            shouldShowNotificationsHint: false,
            routeCardData: [],
            stopCardData: [],
            staticRouteCardData: [],
            staticStopCardData: [],
            loadedLocation: nil
        ))

        var editRsd: RouteStopDirection?
        // We can't test EditFavoritesPage directly until the feature flag is removed
        // let sut = EditFavoritesPage(
        //     viewModel: favoritesVM,
        //     navCallbacks: .companion.empty,
        //     onOpenEditModal: { rsd in editRsd = rsd },
        //     errorBannerVM: MockErrorBannerViewModel(),
        //     toastVM: MockToastViewModel(),
        // )

        let sut = FavoriteRowRightContent(
            leaf: .init(
                lineOrRoute: .Route(route: route),
                stop: stop,
                direction: .init(directionId: 1, route: route),
                routePatterns: [],
                stopIds: Set(),
                upcomingTrips: [],
                alertsHere: [],
                allDataLoaded: true,
                hasSchedulesToday: true,
                subwayServiceStartTime: nil,
                alertsDownstream: [],
                context: .favorites
            ),
            favoriteSettings: .init(),
            onClick: { leaf in editRsd = leaf.routeStopDirection }
        )

        ViewHosting.host(view: sut.withFixedSettings([.notifications: true]))
        defer { ViewHosting.expel() }

        try sut.inspect().findAll(EditFavoriteButton.self)[0].find(ViewType.Button.self).tap()
        XCTAssertEqual(expectedRsd, editRsd)
    }

    @MainActor func testNotificationIcon() {
        let objects = TestData.clone()

        let route: Route = objects.getRoute(id: "Red")
        let stop = objects.getStop(id: "place-asmnl")

        let rsd = RouteStopDirection(route: route.id, stop: stop.id, direction: 1)
        let favoritesVM = MockFavoritesViewModel(initialState: .init(
            awaitingPredictionsAfterBackground: false,
            favorites: [
                rsd: .init(notifications: .init(enabled: true, windows: [])),
            ],
            shouldShowFirstTimeToast: false,
            shouldShowNotificationsHint: false,
            routeCardData: [],
            stopCardData: [],
            staticRouteCardData: [],
            staticStopCardData: [],
            loadedLocation: nil
        ))

        // We can't test EditFavoritesPage directly until the feature flag is removed
        // let sut = EditFavoritesPage(
        //     viewModel: favoritesVM,
        //     navCallbacks: .companion.empty,
        //     onOpenEditModal: { _ in },
        //     errorBannerVM: MockErrorBannerViewModel(),
        //     toastVM: MockToastViewModel(),
        // )
        let sut = FavoriteRowRightContent(
            leaf: .init(
                lineOrRoute: .Route(route: route),
                stop: stop,
                direction: .init(directionId: 1, route: route),
                routePatterns: [],
                stopIds: Set(),
                upcomingTrips: [],
                alertsHere: [],
                allDataLoaded: true,
                hasSchedulesToday: true,
                subwayServiceStartTime: nil,
                alertsDownstream: [],
                context: .favorites
            ),
            favoriteSettings: .init(notifications: .init(enabled: true, windows: [])),
            onClick: { _ in }
        )

        ViewHosting.host(view: sut.withFixedSettings([.notifications: true]))
        defer { ViewHosting.expel() }

        XCTAssertNotNil(try? sut.inspect().find(imageName: "fa-bell-filled"))
    }
}

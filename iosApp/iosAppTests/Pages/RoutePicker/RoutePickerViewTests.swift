//
//  RoutePickerViewTests.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class RoutePickerViewTests: XCTestCase {
    func testDisplaysFavoritesHeader() {
        let objects = ObjectCollectionBuilder()
        objects.route { route in
            route.longName = "Red Line"
            route.type = RouteType.heavyRail
        }

        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(response: GlobalResponse(objects: objects))
        repositories.errorBanner = MockErrorBannerStateRepository()
        loadKoinMocks(repositories: repositories)
        let errorBannerVM = ErrorBannerViewModel(errorRepository: repositories.errorBanner)

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.Root(),
            errorBannerVM: errorBannerVM,
            onOpenRouteDetails: { _, _ in },
            onClose: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Add favorite stops"))
        XCTAssertNotNil(try sut.inspect().find(text: "Done"))
    }

    func testDoneButton() throws {
        let objects = ObjectCollectionBuilder()
        objects.route { route in
            route.longName = "Orange Line"
            route.type = RouteType.heavyRail
        }

        var closeCalled = false

        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(response: GlobalResponse(objects: objects))
        repositories.errorBanner = MockErrorBannerStateRepository()
        loadKoinMocks(repositories: repositories)
        let errorBannerVM = ErrorBannerViewModel(errorRepository: repositories.errorBanner)

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.Root(),
            errorBannerVM: errorBannerVM,
            onOpenRouteDetails: { _, _ in },
            onClose: { closeCalled = true }
        )

        try sut.inspect().find(button: "Done").tap()
        XCTAssertTrue(closeCalled)
    }

    func testDisplaysSubwayHeader() {
        let objects = ObjectCollectionBuilder()
        objects.route { route in
            route.longName = "Green Line"
            route.type = RouteType.lightRail
        }

        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(response: GlobalResponse(objects: objects))
        repositories.errorBanner = MockErrorBannerStateRepository()
        loadKoinMocks(repositories: repositories)
        let errorBannerVM = ErrorBannerViewModel(errorRepository: repositories.errorBanner)

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.Root(),
            errorBannerVM: errorBannerVM,
            onOpenRouteDetails: { _, _ in },
            onClose: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Subway"))
    }

    @MainActor func testDisplaysSubwayRoutes() {
        let objects = ObjectCollectionBuilder()
        objects.route { route in
            route.longName = "Red Line"
            route.type = RouteType.heavyRail
        }
        objects.route { route in
            route.longName = "Mattapan Trolley"
            route.type = RouteType.lightRail
        }

        let gotGlobalData = PassthroughSubject<Void, Never>()
        let repositories = MockRepositories()
        repositories.useObjects(objects: objects)
        repositories.global = MockGlobalRepository(
            response: GlobalResponse(objects: objects),
            onGet: { gotGlobalData.send() }
        )
        repositories.errorBanner = MockErrorBannerStateRepository()
        loadKoinMocks(repositories: repositories)
        let errorBannerVM = ErrorBannerViewModel(errorRepository: repositories.errorBanner)

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.Root(),
            errorBannerVM: errorBannerVM,
            onOpenRouteDetails: { _, _ in },
            onClose: {}
        )

        let exp = sut.inspection.inspect(onReceive: gotGlobalData, after: 1) { view in
            XCTAssertNotNil(try view.find(text: "Red Line"))
            XCTAssertNotNil(try view.find(text: "Mattapan Trolley"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    @MainActor func testRouteSelection() {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.longName = "Orange Line"
            route.type = RouteType.heavyRail
        }

        var selectedRouteId: String?
        var selectedContext: RouteDetailsContext?

        let gotGlobalData = PassthroughSubject<Void, Never>()
        let repositories = MockRepositories()
        repositories.useObjects(objects: objects)
        repositories.global = MockGlobalRepository(
            response: GlobalResponse(objects: objects),
            onGet: { gotGlobalData.send() }
        )
        repositories.errorBanner = MockErrorBannerStateRepository()
        loadKoinMocks(repositories: repositories)
        let errorBannerVM = ErrorBannerViewModel(errorRepository: repositories.errorBanner)

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.Root(),
            errorBannerVM: errorBannerVM,
            onOpenRouteDetails: { routeId, context in
                selectedRouteId = routeId
                selectedContext = context
            },
            onClose: {}
        )

        let exp = sut.inspection.inspect(onReceive: gotGlobalData, after: 1) { view in
            try view.find(button: "Orange Line").tap()
            XCTAssertEqual(route.id, selectedRouteId)
            XCTAssertEqual(RouteDetailsContext.Favorites(), selectedContext)
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }
}

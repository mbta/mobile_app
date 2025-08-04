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
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: {}
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
            onOpenPickerPath: { _, _ in },
            onClose: { closeCalled = true },
            onBack: {}
        )

        try sut.inspect().find(button: "Done").tap()
        XCTAssertTrue(closeCalled)
    }

    func testBackButton() throws {
        let objects = ObjectCollectionBuilder()
        objects.route { route in
            route.longName = " Bus"
            route.type = RouteType.bus
        }

        var backCalled = false

        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(response: GlobalResponse(objects: objects))
        repositories.errorBanner = MockErrorBannerStateRepository()
        loadKoinMocks(repositories: repositories)
        let errorBannerVM = ErrorBannerViewModel(errorRepository: repositories.errorBanner)

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.Bus(),
            errorBannerVM: errorBannerVM,
            onOpenRouteDetails: { _, _ in },
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: { backCalled = true }
        )

        try sut.inspect().find(viewWithAccessibilityLabel: "Back").implicitAnyView().button().tap()
        XCTAssertTrue(backCalled)
    }

    func testDisplaysModePaths() {
        let objects = ObjectCollectionBuilder()

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
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Bus"))
        XCTAssertNotNil(try sut.inspect().find(text: "Silver Line"))
        XCTAssertNotNil(try sut.inspect().find(text: "Commuter Rail"))
        XCTAssertNotNil(try sut.inspect().find(text: "Ferry"))
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
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: {}
        )

        XCTAssertNotNil(try sut.inspect().find(text: "Subway"))
    }

    @MainActor func testDisplaysBus() {
        let objects = ObjectCollectionBuilder()
        objects.route { route in
            route.shortName = "1"
            route.longName = "Harvard Square - Nubian Station"
            route.type = RouteType.bus
        }
        objects.route { route in
            route.shortName = "71"
            route.longName = "Watertown Square - Harvard Station"
            route.type = RouteType.bus
        }
        objects.route { route in
            route.id = "741"
            route.shortName = "SL1"
            route.longName = "Logan Airport Terminals - South Station"
            route.type = RouteType.bus
        }
        objects.route { route in
            route.longName = "Mattapan Trolley"
            route.type = RouteType.lightRail
        }

        let gotGlobalData = PassthroughSubject<Void, Never>()
        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(
            response: GlobalResponse(objects: objects),
            onGet: { gotGlobalData.send() }
        )
        repositories.errorBanner = MockErrorBannerStateRepository()
        loadKoinMocks(repositories: repositories)
        let errorBannerVM = ErrorBannerViewModel(errorRepository: repositories.errorBanner)

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.Bus(),
            errorBannerVM: errorBannerVM,
            onOpenRouteDetails: { _, _ in },
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: {}
        )

        let exp = sut.inspection.inspect(onReceive: gotGlobalData, after: 1) { view in
            XCTAssertNotNil(try view.find(text: "Harvard Square - Nubian Station"))
            XCTAssertNotNil(try view.find(text: "Watertown Square - Harvard Station"))
            XCTAssertNotNil(try view.find(text: "Logan Airport Terminals - South Station"))
            XCTAssertThrowsError(try view.find(text: "Mattapan Trolley"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    @MainActor func testDisplaysSilverLineRoutes() {
        let objects = ObjectCollectionBuilder()
        objects.route { route in
            route.shortName = "66"
            route.longName = "Harvard Square - Nubian Station"
            route.type = RouteType.bus
        }
        objects.route { route in
            route.id = "741"
            route.shortName = "SL1"
            route.longName = "Logan Airport Terminals - South Station"
            route.type = RouteType.bus
        }

        let gotGlobalData = PassthroughSubject<Void, Never>()
        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(
            response: GlobalResponse(objects: objects),
            onGet: { gotGlobalData.send() }
        )
        repositories.errorBanner = MockErrorBannerStateRepository()
        loadKoinMocks(repositories: repositories)
        let errorBannerVM = ErrorBannerViewModel(errorRepository: repositories.errorBanner)

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.Silver(),
            errorBannerVM: errorBannerVM,
            onOpenRouteDetails: { _, _ in },
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: {}
        )

        let exp = sut.inspection.inspect(onReceive: gotGlobalData, after: 1) { view in
            XCTAssertThrowsError(try view.find(text: "Harvard Square - Nubian Station"))
            XCTAssertNotNil(try view.find(text: "Logan Airport Terminals - South Station"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    @MainActor func testDisplaysCommuterRailRoutes() {
        let objects = ObjectCollectionBuilder()
        objects.route { route in
            route.longName = "Providence/Stoughton Line"
            route.type = RouteType.commuterRail
        }

        let gotGlobalData = PassthroughSubject<Void, Never>()
        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(
            response: GlobalResponse(objects: objects),
            onGet: { gotGlobalData.send() }
        )
        repositories.errorBanner = MockErrorBannerStateRepository()
        loadKoinMocks(repositories: repositories)
        let errorBannerVM = ErrorBannerViewModel(errorRepository: repositories.errorBanner)

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.CommuterRail(),
            errorBannerVM: errorBannerVM,
            onOpenRouteDetails: { _, _ in },
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: {}
        )

        let exp = sut.inspection.inspect(onReceive: gotGlobalData, after: 1) { view in
            XCTAssertNotNil(try view.find(text: "Providence/Stoughton Line"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    @MainActor func testDisplaysFerryRoutes() {
        let objects = ObjectCollectionBuilder()
        objects.route { route in
            route.longName = "Lynn Ferry"
            route.type = RouteType.ferry
        }

        let gotGlobalData = PassthroughSubject<Void, Never>()
        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(
            response: GlobalResponse(objects: objects),
            onGet: { gotGlobalData.send() }
        )
        repositories.errorBanner = MockErrorBannerStateRepository()
        loadKoinMocks(repositories: repositories)
        let errorBannerVM = ErrorBannerViewModel(errorRepository: repositories.errorBanner)

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.Ferry(),
            errorBannerVM: errorBannerVM,
            onOpenRouteDetails: { _, _ in },
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: {}
        )

        let exp = sut.inspection.inspect(onReceive: gotGlobalData, after: 1) { view in
            XCTAssertNotNil(try view.find(text: "Lynn Ferry"))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
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
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: {}
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
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: {}
        )

        let exp = sut.inspection.inspect(onReceive: gotGlobalData, after: 1) { view in
            try view.find(button: "Orange Line").tap()
            XCTAssertEqual(route.id, selectedRouteId)
            XCTAssertEqual(RouteDetailsContext.Favorites(), selectedContext)
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    @MainActor func testFilterInputRequests() {
        let objects = ObjectCollectionBuilder()
        let route1 =
            objects.route { route in
                route.shortName = "1"
                route.longName = "Harvard Square - Nubian Station"
                route.type = RouteType.bus
            }
        let route71 =
            objects.route { route in
                route.shortName = "71"
                route.longName = "Watertown Square - Harvard Station"
                route.type = RouteType.bus
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
        let mockSearchVM = MockSearchRoutesViewModel(
            initialState: SearchRoutesViewModel.StateResults(routeIds: [route1.id])
        )

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.Bus(),
            errorBannerVM: errorBannerVM,
            searchRoutesViewModel: mockSearchVM,
            onOpenRouteDetails: { _, _ in },
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: {}
        )

        let exp = sut.inspection.inspect(onReceive: gotGlobalData, after: 1) { view in
            XCTAssertNoThrow(try view.find(text: "Filter routes"))
            XCTAssertNoThrow(try view.find(text: "To find stops, select a route first"))
            XCTAssertNoThrow(try view.find(text: route1.longName))
            XCTAssertThrowsError(try view.find(text: route71.longName))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    @MainActor func testNoFilterResults() {
        let objects = ObjectCollectionBuilder()
        let route1 =
            objects.route { route in
                route.shortName = "1"
                route.longName = "Harvard Square - Nubian Station"
                route.type = RouteType.bus
            }
        let route71 =
            objects.route { route in
                route.shortName = "71"
                route.longName = "Watertown Square - Harvard Station"
                route.type = RouteType.bus
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
        let mockSearchVM = MockSearchRoutesViewModel(
            initialState: SearchRoutesViewModel.StateResults(routeIds: [])
        )

        let sut = RoutePickerView(
            context: RouteDetailsContext.Favorites(),
            path: RoutePickerPath.Bus(),
            errorBannerVM: errorBannerVM,
            searchRoutesViewModel: mockSearchVM,
            onOpenRouteDetails: { _, _ in },
            onOpenPickerPath: { _, _ in },
            onClose: {},
            onBack: {}
        )

        let exp = sut.inspection.inspect(onReceive: gotGlobalData, after: 1) { view in
            XCTAssertNoThrow(try view.find(text: "Filter routes"))
            XCTAssertNoThrow(try view.find(text: "To find stops, select a route first"))
            XCTAssertNoThrow(try view.find(text: "No matching bus routes"))
            XCTAssertThrowsError(try view.find(text: route1.longName))
            XCTAssertThrowsError(try view.find(text: route71.longName))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 3)
    }
}

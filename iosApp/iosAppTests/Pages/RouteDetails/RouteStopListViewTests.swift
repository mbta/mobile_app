//
//  RouteStopListViewTests.swift
//  iosAppTests
//
//  Created by Melody Horn on 7/8/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class RouteStopListViewTests: XCTestCase {
    @MainActor func testDisplaysEverything() throws {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { $0.name = "Stop 1" }
        let stop2 = objects.stop { $0.name = "Stop 2" }
        let stop3 = objects.stop { $0.name = "Stop 3" }
        let mainRoute = objects.route { route in
            route.directionNames = ["West", "East"]
            route.directionDestinations = ["Here", "There"]
            route.longName = "Mauve Line"
            route.type = .heavyRail
        }
        objects.routePattern(route: mainRoute) { pattern in
            pattern.directionId = 0
            pattern.typicality = .typical
            pattern.representativeTrip { $0.stopIds = [stop1.id, stop2.id, stop3.id] }
        }
        objects.routePattern(route: mainRoute) { pattern in
            pattern.directionId = 1
            pattern.typicality = .typical
        }
        let connectingRoute = objects.route { route in
            route.shortName = "32"
            route.type = .bus
        }
        objects.routePattern(route: connectingRoute) { pattern in
            pattern.typicality = .typical
            pattern.representativeTrip { $0.stopIds = [stop2.id] }
        }
        var clicks: [RouteDetailsRowContext] = []
        var backTapped = false
        var closeTapped = false

        let gotRouteStops = PassthroughSubject<Void, Never>()
        let repositories = MockRepositories()
        repositories.useObjects(objects: objects)
        repositories.routeStops = MockRouteStopsRepository(
            segments: [RouteBranchSegment.companion.of(stopIds: [stop1.id, stop2.id, stop3.id])],
            routeId: mainRoute.id,
            onGet: { _, _ in gotRouteStops.send() }
        )
        HelpersKt.loadKoinMocks(repositories: repositories)
        let errorBannerVM = ViewModelDI().errorBanner

        let sut = RouteStopListView(
            lineOrRoute: .route(mainRoute),
            context: .Details.shared,
            globalData: .init(objects: objects),
            onClick: { clicks.append($0) },
            pushNavEntry: { _ in },
            navCallbacks: .init(
                onBack: { backTapped = true },
                onClose: { closeTapped = true },
                backButtonPresentation: .header
            ),
            errorBannerVM: errorBannerVM,
            rightSideContent: { switch $0 {
            case let .details(stop: stop): return Text(verbatim: "rightSideContent for \(stop.name)")
            default: XCTFail("Wrong row context provided")
                return Text(verbatim: "")
            }}
        )

        let exp = sut.inspection.inspect(onReceive: gotRouteStops, after: 1) { view in
            XCTAssertNil(
                try? view.find(where: { (try? $0.modifier(LoadingPlaceholderModifier.self)) != nil }).pathToRoot,
                "has not finished loading"
            )
            XCTAssertNotNil(try view.find(text: mainRoute.longName))
            XCTAssertNotNil(try view.find(text: "Westbound to"))
            XCTAssertNotNil(try view.find(text: "Here"))
            XCTAssertNotNil(try view.find(text: "Eastbound to"))
            XCTAssertNotNil(try view.find(text: "There"))
            XCTAssertNotNil(try view.find(text: stop1.name))
            XCTAssertNotNil(try view.find(text: stop2.name))
            XCTAssertNotNil(try view.find(text: stop3.name))
            XCTAssertNotNil(try view.find(text: "rightSideContent for \(stop1.name)"))
            XCTAssertNotNil(try view.find(text: "rightSideContent for \(stop2.name)"))
            XCTAssertNotNil(try view.find(text: "rightSideContent for \(stop3.name)"))
            XCTAssertNotNil(try view.find(text: connectingRoute.shortName))

            try view.find(text: stop2.name).find(ViewType.Button.self, relation: .parent).tap()
            XCTAssertEqual([RouteDetailsRowContext.details(stop: stop2)], clicks)

            try view.find(ViewType.Button.self, where: { try $0.accessibilityLabel().string() == "Back" }).tap()
            XCTAssertTrue(backTapped)

            try view.find(button: "Done").tap()
            XCTAssertTrue(closeTapped)
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    @MainActor func testSelectsWithinLine() throws {
        let objects = ObjectCollectionBuilder()
        let line = objects.line()
        let route1 = objects.route { route in
            route.lineId = line.id.idText
            route.type = .bus
            route.shortName = "1"
            route.directionDestinations = ["One", ""]
        }
        let route2 = objects.route { route in
            route.lineId = line.id.idText
            route.type = .bus
            route.shortName = "2"
            route.directionDestinations = ["Two", ""]
        }
        let route3 = objects.route { route in
            route.lineId = line.id.idText
            route.type = .bus
            route.shortName = "3"
            route.directionDestinations = ["Three", ""]
        }
        objects.routePattern(route: route1) { $0.directionId = 0 }

        var lastSelectedRoute: Route.Id?

        let getRouteStopsSubject = PassthroughSubject<Void, Never>()
        let repositories = MockRepositories()
        repositories.useObjects(objects: objects)
        repositories.routeStops = MockRouteStopsRepository(
            segments: [],
            onGet: { routeId, _ in lastSelectedRoute = routeId; getRouteStopsSubject.send() }
        )
        HelpersKt.loadKoinMocks(repositories: repositories)
        let errorBannerVM = ViewModelDI().errorBanner

        let sut = RouteStopListView(
            lineOrRoute: .line(line, [route1, route2, route3]),
            context: .Details.shared,
            globalData: .init(objects: objects),
            onClick: { _ in },
            pushNavEntry: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: errorBannerVM,
            defaultSelectedRouteId: route2.id,
            rightSideContent: { _ in EmptyView() }
        )

        let exp1 = sut.inspection.inspect(onReceive: getRouteStopsSubject, after: 0.2) { view in
            XCTAssertEqual(route2.id, lastSelectedRoute)
            try view.find(button: route3.shortName).tap()
        }
        let exp2 = sut.inspection.inspect(onReceive: getRouteStopsSubject.dropFirst(), after: 0.2) { view in
            XCTAssertEqual(route3.id, lastSelectedRoute)
            try view.find(button: "One").tap()
        }
        let exp3 = sut.inspection.inspect(onReceive: getRouteStopsSubject.dropFirst(2), after: 0.2) { _ in
            XCTAssertEqual(route1.id, lastSelectedRoute)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp1, exp2, exp3], timeout: 5)
    }

    @MainActor func testCollapsesNonTypicalStops() throws {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { $0.name = "Stop 1" }
        let stop2 = objects.stop { $0.name = "Stop 2" }
        let stop3NonTypical = objects.stop { $0.name = "Stop 3" }
        let stop4NonTypical = objects.stop { $0.name = "Stop 4" }
        let mainRoute = objects.route { route in
            route.directionNames = ["West", "East"]
            route.directionNames = ["Here", "There"]
            route.longName = "Mauve Line"
            route.type = .heavyRail
        }
        objects.routePattern(route: mainRoute) { pattern in
            pattern.directionId = 0
            pattern.typicality = .typical
            pattern.representativeTrip { $0.stopIds = [stop1.id, stop2.id] }
        }
        objects.routePattern(route: mainRoute) { pattern in
            pattern.directionId = 0
            pattern.typicality = .deviation
            pattern.representativeTrip { $0.stopIds = [stop1.id, stop2.id, stop3NonTypical.id, stop4NonTypical.id] }
        }

        let gotRouteStops = PassthroughSubject<Void, Never>()
        let repositories = MockRepositories()
        repositories.useObjects(objects: objects)
        repositories.routeStops = MockRouteStopsRepository(
            segments: [
                RouteBranchSegment.companion.of(stopIds: [stop1.id, stop2.id]),
                RouteBranchSegment.companion.of(
                    stopIds: [stop3NonTypical.id, stop4NonTypical.id],
                    isTypical: false
                ),
            ],
            routeId: mainRoute.id,
            onGet: { _, _ in gotRouteStops.send() }
        )
        HelpersKt.loadKoinMocks(repositories: repositories)
        let errorBannerVM = ViewModelDI().errorBanner

        let sut = RouteStopListView(
            lineOrRoute: .route(mainRoute),
            context: .Details.shared,
            globalData: .init(objects: objects),
            onClick: { _ in },
            pushNavEntry: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: errorBannerVM,
            rightSideContent: { _ in EmptyView() }
        )

        let exp = sut.inspection.inspect(onReceive: gotRouteStops, after: 1) { view in
            XCTAssertNil(
                try? view.find(where: { (try? $0.modifier(LoadingPlaceholderModifier.self)) != nil }).pathToRoot,
                "has not finished loading"
            )
            XCTAssertNotNil(try view.find(text: stop1.name))
            XCTAssertNotNil(try view.find(text: stop2.name))
            XCTAssertNil(try? view.find(text: stop1.name).find(ViewType.DisclosureGroup.self, relation: .parent))
            XCTAssertNotNil(try view.find(text: stop3NonTypical.name))
            XCTAssertNotNil(try view.find(text: stop3NonTypical.name).find(
                ViewType.DisclosureGroup.self,
                relation: .parent
            ))
            XCTAssertEqual(
                "2 less common stops",
                try view.find(ViewType.DisclosureGroup.self).labelView().find(ViewType.Text.self).string()
            )
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }

    @MainActor func testDoesntCollapseWhenOnlyNonTypical() throws {
        let objects = ObjectCollectionBuilder()
        let stop1 = objects.stop { $0.name = "Stop 1" }
        let stop2 = objects.stop { $0.name = "Stop 2" }
        let mainRoute = objects.route { route in
            route.directionNames = ["West", "East"]
            route.directionNames = ["Here", "There"]
            route.longName = "Mauve Line"
            route.type = .heavyRail
        }
        objects.routePattern(route: mainRoute) { pattern in
            pattern.directionId = 0
            pattern.typicality = .deviation
            pattern.representativeTrip { $0.stopIds = [stop1.id, stop2.id] }
        }

        let gotRouteStops = PassthroughSubject<Void, Never>()
        let repositories = MockRepositories()
        repositories.useObjects(objects: objects)
        repositories.routeStops = MockRouteStopsRepository(
            segments: [.companion.of(stopIds: [stop1.id, stop2.id], isTypical: false)],
            routeId: mainRoute.id,
            onGet: { _, _ in gotRouteStops.send() }
        )
        HelpersKt.loadKoinMocks(repositories: repositories)
        let errorBannerVM = ViewModelDI().errorBanner

        let sut = RouteStopListView(
            lineOrRoute: .route(mainRoute),
            context: .Details.shared,
            globalData: .init(objects: objects),
            onClick: { _ in },
            pushNavEntry: { _ in },
            navCallbacks: .companion.empty,
            errorBannerVM: errorBannerVM,
            rightSideContent: { _ in EmptyView() }
        )

        let exp = sut.inspection.inspect(onReceive: gotRouteStops, after: 1) { view in
            XCTAssertNil(
                try? view.find(where: { (try? $0.modifier(LoadingPlaceholderModifier.self)) != nil }).pathToRoot,
                "has not finished loading"
            )
            XCTAssertNotNil(try view.find(text: stop1.name))
            XCTAssertNotNil(try view.find(text: stop2.name))
            XCTAssertThrowsError(try view.find(ViewType.DisclosureGroup.self))
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [exp], timeout: 2)
    }
}

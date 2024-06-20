//
//  NearbyTransitPageViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
@testable import iosApp
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest
@_spi(Experimental) import MapboxMaps

// swiftlint:disable:next type_body_length
final class NearbyTransitPageViewTests: XCTestCase {
    struct NotUnderTestError: Error {}

    private let pinnedRoutesRepository = MockPinnedRoutesRepository()

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testMessageWhenManuallyCentering() throws {
        let viewportProvider = ViewportProvider(viewport: nil,
                                                isManuallyCentering: true)
        let globalFetcher = GlobalFetcher(backend: IdleBackend())
        let sut = NearbyTransitPageView(
            globalFetcher: globalFetcher,
            nearbyVM: .init(),
            viewportProvider: viewportProvider
        )
        XCTAssertNotNil(try sut.inspect().find(text: "select location"))
    }

    func testClearsNearbyStateWhenManuallyCentering() throws {
        let viewportProvider = ViewportProvider(viewport: nil,
                                                isManuallyCentering: false)
        let nearbyVM: NearbyViewModel = .init()
        nearbyVM.nearbyState = .init(loadedLocation: .init(latitude: 0, longitude: 0),
                                     nearbyByRouteAndStop: .init(data: []))

        let globalFetcher = GlobalFetcher(backend: IdleBackend())
        let sut = NearbyTransitPageView(
            globalFetcher: globalFetcher,
            nearbyVM: nearbyVM,
            viewportProvider: viewportProvider
        )
        try sut.inspect().find(ViewType.VStack.self).callOnChange(newValue: true)

        XCTAssertNil(nearbyVM.nearbyState.loadedLocation)
        XCTAssertNil(nearbyVM.nearbyState.nearbyByRouteAndStop)
    }

    @MainActor func testReloadsWhenLocationChanges() throws {
        class FakeNearbyVM: NearbyViewModel {
            let expectation: XCTestExpectation
            let closure: (CLLocationCoordinate2D) -> Void

            init(_ expectation: XCTestExpectation, _ closure: @escaping (CLLocationCoordinate2D) -> Void) {
                self.expectation = expectation
                self.closure = closure
                super.init()
            }

            override func getNearby(global _: GlobalResponse, location: CLLocationCoordinate2D) {
                closure(location)
                expectation.fulfill()
            }
        }

        let getNearbyExpectation = expectation(description: "getNearby")
        let newCameraState = CameraState(
            center: CLLocationCoordinate2D(latitude: 0.0, longitude: 0.0),
            padding: .zero,
            zoom: ViewportProvider.Defaults.zoom,
            bearing: 0.0,
            pitch: 0.0
        )
        let fakeVM = FakeNearbyVM(getNearbyExpectation) { location in
            XCTAssertEqual(location, newCameraState.center)
        }
        let viewportProvider = ViewportProvider(viewport: .followPuck(zoom: ViewportProvider.Defaults.zoom))
        let globalFetcher = GlobalFetcher(backend: IdleBackend())
        globalFetcher.response = .init(
            lines: [:],
            patternIdsByStop: [:],
            routes: [:],
            routePatterns: [:],
            stops: [:],
            trips: [:]
        )
        let sut = NearbyTransitPageView(
            globalFetcher: globalFetcher,
            nearbyVM: fakeVM,
            viewportProvider: viewportProvider
        )
        let hasAppeared = sut.inspection.inspect { view in
            XCTAssertNil(try view.find(NearbyTransitView.self).actualView().location)
            try view.find(NearbyTransitView.self).vStack().callOnChange(newValue: newCameraState.center)
        }

        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, getNearbyExpectation], timeout: 10)
    }

    @MainActor func testNoReloadWhenNotVisbleAndLocationChanges() throws {
        class FakeNearbyVM: NearbyViewModel {
            let expectation: XCTestExpectation

            init(_ expectation: XCTestExpectation, navigationStack: [SheetNavigationStackEntry]) {
                self.expectation = expectation
                super.init(navigationStack: navigationStack)
            }

            override func getNearby(global _: GlobalResponse, location _: CLLocationCoordinate2D) {
                expectation.fulfill()
            }
        }

        let getNearbyNotCalledExpectation = expectation(description: "getNearbyNotCalled")
        getNearbyNotCalledExpectation.isInverted = true
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let viewportProvider = ViewportProvider(viewport: .followPuck(zoom: ViewportProvider.Defaults.zoom))
        let globalFetcher = GlobalFetcher(backend: IdleBackend())
        globalFetcher.response = .init(
            lines: [:],
            patternIdsByStop: [:],
            routes: [:],
            routePatterns: [:],
            stops: [:],
            trips: [:]
        )
        let fakeVM = FakeNearbyVM(getNearbyNotCalledExpectation, navigationStack: [.stopDetails(stop, nil)])
        let sut = NearbyTransitPageView(
            globalFetcher: globalFetcher,
            nearbyVM: fakeVM,
            viewportProvider: viewportProvider
        )

        let newCameraState = CameraState(
            center: CLLocationCoordinate2D(latitude: 0.0, longitude: 0.0),
            padding: .zero,
            zoom: ViewportProvider.Defaults.zoom,
            bearing: 0.0,
            pitch: 0.0
        )
        let appearancePublisher = PassthroughSubject<Bool, Never>()
        let hasAppeared = sut.inspection.inspect(after: 0.2) { view in
            XCTAssertNil(try view.find(NearbyTransitView.self).actualView().location)
            try view.actualView().viewportProvider.updateCameraState(newCameraState)
            appearancePublisher.send(true)
        }
        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, getNearbyNotCalledExpectation], timeout: 5)
    }
}

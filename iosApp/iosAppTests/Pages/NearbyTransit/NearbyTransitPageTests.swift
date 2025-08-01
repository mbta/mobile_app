//
//  NearbyTransitPageTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
@testable import iosApp
@_spi(Experimental) import MapboxMaps
import Shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

// swiftlint:disable:next type_body_length
final class NearbyTransitPageTests: XCTestCase {
    private let pinnedRoutesRepository = MockPinnedRoutesRepository()
    private let noNearbyStops = { NoNearbyStopsView(onOpenSearch: {}, onPanToDefaultCenter: {}) }

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testClearsNearbyStateWhenManuallyCentering() throws {
        let viewportProvider = ViewportProvider(
            viewport: nil,
            isManuallyCentering: false
        )
        let nearbyVM = NearbyViewModel()
        nearbyVM.nearbyState = .init(loadedLocation: .init(latitude: 0, longitude: 0))
        nearbyVM.routeCardData = []

        let sut = NearbyTransitPage(
            errorBannerVM: .init(),
            nearbyVM: nearbyVM,
            viewportProvider: viewportProvider,
            noNearbyStops: noNearbyStops
        )
        try sut.inspect().find(ViewType.VStack.self).callOnChange(newValue: true)

        XCTAssertNil(nearbyVM.nearbyState.loadedLocation)
        XCTAssertNil(nearbyVM.routeCardData)
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

            override func getNearbyStops(global _: GlobalResponse, location: CLLocationCoordinate2D) {
                debugPrint("ViewModel getting nearby")
                closure(location)
                expectation.fulfill()
            }
        }

        let globalDataLoaded = PassthroughSubject<Void, Never>()

        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(onGet: { globalDataLoaded.send() })
        loadKoinMocks(repositories: repositories)

        let getNearbyExpectation = expectation(description: "getNearby")
        getNearbyExpectation.assertForOverFulfill = false
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
        let sut = NearbyTransitPage(
            errorBannerVM: .init(),
            nearbyVM: fakeVM,
            viewportProvider: viewportProvider,
            noNearbyStops: noNearbyStops
        )
        let hasAppeared = sut.inspection.inspect(onReceive: globalDataLoaded) { view in
            XCTAssertNil(try view.find(NearbyTransitView.self).actualView().location)
            try view.find(NearbyTransitView.self).actualView().globalData = .init(
                objects: .init(),
                patternIdsByStop: [:]
            )
            try view.find(NearbyTransitView.self).implicitAnyView().vStack()
                .callOnChange(newValue: newCameraState.center)
        }

        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, getNearbyExpectation], timeout: 10)
    }

    @MainActor func testNoReloadWhenNotVisbleAndLocationChanges() throws {
        class FakeNearbyVM: NearbyViewModel {
            let expectation: XCTestExpectation

            init(_ expectation: XCTestExpectation, navigationStack: [SheetNavigationStackEntry]) {
                self.expectation = expectation
                super.init(navigationStack: navigationStack)
            }

            override func getNearbyStops(global _: GlobalResponse, location _: CLLocationCoordinate2D) {
                expectation.fulfill()
            }
        }

        let getNearbyNotCalledExpectation = expectation(description: "getNearbyNotCalled")
        getNearbyNotCalledExpectation.isInverted = true
        let objects = ObjectCollectionBuilder()
        let stop = objects.stop { _ in }

        let viewportProvider = ViewportProvider(viewport: .followPuck(zoom: ViewportProvider.Defaults.zoom))
        let fakeVM = FakeNearbyVM(getNearbyNotCalledExpectation, navigationStack: [
            .stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil),
        ])
        let sut = NearbyTransitPage(
            errorBannerVM: .init(),
            nearbyVM: fakeVM,
            viewportProvider: viewportProvider,
            noNearbyStops: noNearbyStops
        )

        let newCameraState = CameraState(
            center: CLLocationCoordinate2D(latitude: 0.0, longitude: 0.0),
            padding: .zero,
            zoom: ViewportProvider.Defaults.zoom,
            bearing: 0.0,
            pitch: 0.0
        )
        let appearancePublisher = PassthroughSubject<Bool, Never>()
        let hasAppeared = sut.inspection.inspect(after: 1) { view in
            XCTAssertNil(try view.find(NearbyTransitView.self).actualView().location)
            try view.actualView().viewportProvider.updateCameraState(newCameraState)
            appearancePublisher.send(true)
        }
        ViewHosting.host(view: sut.withFixedSettings([:]))
        wait(for: [hasAppeared, getNearbyNotCalledExpectation], timeout: 5)
    }
}

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
@_spi(Experimental) import MapboxMaps
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

// swiftlint:disable:next type_body_length
final class NearbyTransitPageViewTests: XCTestCase {
    private let pinnedRoutesRepository = MockPinnedRoutesRepository()

    override func setUp() {
        executionTimeAllowance = 60
    }

    func testMessageWhenManuallyCentering() throws {
        let viewportProvider = ViewportProvider(viewport: nil,
                                                isManuallyCentering: true)
        let sut = NearbyTransitPageView(
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

        let sut = NearbyTransitPageView(
            nearbyVM: nearbyVM,
            viewportProvider: viewportProvider
        )
        try sut.inspect().find(ViewType.VStack.self).callOnChange(newValue: true)

        XCTAssertNil(nearbyVM.nearbyState.loadedLocation)
        XCTAssertNil(nearbyVM.nearbyState.nearbyByRouteAndStop)
    }

    @MainActor func testReloadsWhenLocationChanges() throws {
        class FakeGlobalRepository: IGlobalRepository {
            let notifier: any Subject<Void, Never>

            init(notifier: any Subject<Void, Never>) {
                self.notifier = notifier
            }

            func __getGlobalData() async throws -> ApiResult<GlobalResponse> {
                debugPrint("FakeGlobalRepo getting global")
                notifier.send()
                return ApiResultOk(data: GlobalResponse(objects: .init(), patternIdsByStop: [:]))
            }
        }

        class FakeNearbyVM: NearbyViewModel {
            let expectation: XCTestExpectation
            let closure: (CLLocationCoordinate2D) -> Void

            init(_ expectation: XCTestExpectation, _ closure: @escaping (CLLocationCoordinate2D) -> Void) {
                self.expectation = expectation
                self.closure = closure
                super.init()
            }

            override func getNearby(global _: GlobalResponse, location: CLLocationCoordinate2D) {
                debugPrint("ViewModel getting nearby")
                closure(location)
                expectation.fulfill()
            }
        }

        let globalDataLoaded = PassthroughSubject<Void, Never>()

        loadKoinMocks(repositories: MockRepositories.companion
            .buildWithDefaults(global: FakeGlobalRepository(notifier: globalDataLoaded)))

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
        let sut = NearbyTransitPageView(
            nearbyVM: fakeVM,
            viewportProvider: viewportProvider
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
        let fakeVM = FakeNearbyVM(getNearbyNotCalledExpectation, navigationStack: [.stopDetails(stop, nil)])
        let sut = NearbyTransitPageView(
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
        let hasAppeared = sut.inspection.inspect(after: 1) { view in
            XCTAssertNil(try view.find(NearbyTransitView.self).actualView().location)
            try view.actualView().viewportProvider.updateCameraState(newCameraState)
            appearancePublisher.send(true)
        }
        ViewHosting.host(view: sut)
        wait(for: [hasAppeared, getNearbyNotCalledExpectation], timeout: 5)
    }
}

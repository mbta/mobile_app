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
    private let noNearbyStops = { NoNearbyStopsView(onOpenSearch: {}, onPanToDefaultCenter: {}) }

    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testReloadsWhenLocationChanges() {
        let globalDataLoaded = PassthroughSubject<Void, Never>()

        let repositories = MockRepositories()
        repositories.global = MockGlobalRepository(onGet: { globalDataLoaded.send() })
        loadKoinMocks(repositories: repositories)

        let newCameraState = CameraState(
            center: CLLocationCoordinate2D(latitude: 0.0, longitude: 0.0),
            padding: .zero,
            zoom: ViewportProvider.Defaults.zoom,
            bearing: 0.0,
            pitch: 0.0
        )
        let locationChangeExpectation = expectation(description: "getNearbyNotCalled")
        let mockNearbyVM = MockNearbyViewModel(initialState: .init())
        mockNearbyVM.onSetLocation = { location in
            XCTAssertEqual(location?.coordinate, newCameraState.center)
            locationChangeExpectation.fulfill()
        }
        let viewportProvider = ViewportProvider(viewport: .followPuck(zoom: ViewportProvider.Defaults.zoom))
        let sut = NearbyTransitPage(
            alerts: .init(alerts: [:]),
            errorBannerVM: MockErrorBannerViewModel(),
            nearbyVM: mockNearbyVM,
            navManager: .init(),
            noNearbyStops: noNearbyStops,
        )
        let hasAppeared = sut.inspection.inspect(onReceive: globalDataLoaded) { view in
            XCTAssertNil(try view.find(NearbyTransitView.self).actualView().location)
            try view.find(NearbyTransitView.self).actualView().globalData = .init(
                objects: .init(),
                patternIdsByStop: [:]
            )
            try view.find(NearbyTransitView.self).find(ViewType.VStack.self)
                .callOnChange(newValue: newCameraState.center)
        }

        ViewHosting.host(view: sut.environmentObject(viewportProvider).withFixedSettings([:]))
        wait(for: [hasAppeared, locationChangeExpectation], timeout: 10)
    }

    @MainActor func testErrorBanner() {
        let viewportProvider = ViewportProvider(viewport: .followPuck(zoom: ViewportProvider.Defaults.zoom))

        let sut = NearbyTransitPage(
            alerts: .init(alerts: [:]),
            errorBannerVM: MockErrorBannerViewModel(
                initialState: .init(
                    loadingWhenPredictionsStale: false,
                    errorState: .DataError(messages: [], details: [], action: {})
                )
            ),
            nearbyVM: MockNearbyViewModel(),
            navManager: .init(),
            noNearbyStops: noNearbyStops
        ).environmentObject(viewportProvider).withFixedSettings([:])

        XCTAssertNotNil(try sut.inspect().find(ErrorBanner.self))
    }
}

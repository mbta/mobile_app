//
//  ContentViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 12/28/23.
//  Copyright © 2023 orgName. All rights reserved.
//
import Combine
import Foundation
@testable import iosApp
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

final class ContentViewTests: XCTestCase {
    struct NotUnderTestError: Error {}

    override func setUp() {
        executionTimeAllowance = 60
        HelpersKt
            .loadKoinMocks(repositories: MockRepositories.companion
                .buildWithDefaults(schedules: MockScheduleRepository()))
    }

    override func tearDown() {
        HelpersKt.loadDefaultRepoModules()
    }

    func testDisconnectsSocketAfterBackgrounding() throws {
        let connectedExpectation = expectation(description: "Socket has connected")
        let disconnectedExpectation = expectation(description: "Socket has disconnected")

        let fakeSocketWithExpectations = FakeSocket(connectedExpectation: connectedExpectation,
                                                    disconnectedExpectation: disconnectedExpectation)

        let sut = ContentView(contentVM: .init())
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(BackendProvider(backend: IdleBackend()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(SocketProvider(socket: fakeSocketWithExpectations))
            .environmentObject(VehiclesFetcher(socket: FakeSocket()))
            .environmentObject(ViewportProvider())

        ViewHosting.host(view: sut)

        wait(for: [connectedExpectation], timeout: 1)
        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.background)
        wait(for: [disconnectedExpectation], timeout: 5)
    }

    func testReconnectsSocketAfterBackgroundingAndReactivating() throws {
        let disconnectedExpectation = expectation(description: "Socket has disconnected")
        let connectedExpectation = expectation(description: "Socket has connected")
        connectedExpectation.expectedFulfillmentCount = 2
        connectedExpectation.assertForOverFulfill = true

        let fakeSocketWithExpectations = FakeSocket(connectedExpectation: connectedExpectation,
                                                    disconnectedExpectation: disconnectedExpectation)

        let sut = ContentView(contentVM: .init())
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(BackendProvider(backend: IdleBackend()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(SocketProvider(socket: fakeSocketWithExpectations))
            .environmentObject(VehiclesFetcher(socket: FakeSocket()))
            .environmentObject(ViewportProvider())

        ViewHosting.host(view: sut)

        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.background)
        wait(for: [disconnectedExpectation], timeout: 1)
        try sut.inspect().vStack().callOnChange(newValue: ScenePhase.active)
        wait(for: [connectedExpectation], timeout: 1)
    }

    func testFetchesConfigIfFeatureFlagEnabled() throws {
        let configFetchedExpectation = XCTestExpectation(description: "config fetched")

        let fakeVM = FakeContentVM(dynamicMapKeyEnabled: true,
                                   loadConfigCallback: { configFetchedExpectation.fulfill() })
        let sut = ContentView(contentVM: fakeVM)

        ViewHosting.host(view: sut
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(BackendProvider(backend: IdleBackend()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(SocketProvider(socket: FakeSocket()))
            .environmentObject(VehiclesFetcher(socket: FakeSocket()))
            .environmentObject(ViewportProvider()))

        wait(for: [configFetchedExpectation], timeout: 5)
    }

    func testSetsMapboxTokenConfigOnConfigChange() throws {
        let configFetchedExpectation = XCTestExpectation(description: "config fetched")

        let fakeVM = FakeContentVM(dynamicMapKeyEnabled: true, mapboxTokenConfigured: false)
        let sut = ContentView(contentVM: fakeVM)

        ViewHosting.host(view: sut
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(BackendProvider(backend: IdleBackend()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(SocketProvider(socket: FakeSocket()))
            .environmentObject(VehiclesFetcher(socket: FakeSocket()))
            .environmentObject(ViewportProvider()))

        let newConfig: ApiResult<ConfigResponse>? = ApiResultOk(data: .init(mapboxPublicToken: "FAKE_TOKEN"))

        try sut.inspect().vStack()
            .callOnChange(newValue: newConfig)
        XCTAssertTrue(fakeVM.mapboxTokenConfigured)
    }

    func testShowsMapWhenFeatureFlagDisabled() throws {
        let fakeVM: ContentViewModel = FakeContentVM(dynamicMapKeyEnabled: false)
        let sut = ContentView(contentVM: fakeVM)
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(BackendProvider(backend: IdleBackend()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(SocketProvider(socket: FakeSocket()))
            .environmentObject(VehiclesFetcher(socket: FakeSocket()))
            .environmentObject(ViewportProvider())
        XCTAssertNotNil(try sut.inspect().find(HomeMapView.self))
    }

    func testHidesMapBeforeTokenInitWhenFeatureFlagEnabled() throws {
        let sut = ContentView(contentVM: FakeContentVM(dynamicMapKeyEnabled: true, mapboxTokenConfigured: false))
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(BackendProvider(backend: IdleBackend()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(SocketProvider(socket: FakeSocket()))
            .environmentObject(VehiclesFetcher(socket: FakeSocket()))
            .environmentObject(ViewportProvider())
        XCTAssertNotNil(try sut.inspect().find(viewWithAccessibilityIdentifier: "mapLoadingCard"))
    }

    func testShowsMapWhenTokenInitAndFeatureFlagEnabled() throws {
        let sut = ContentView(contentVM: FakeContentVM(dynamicMapKeyEnabled: true, mapboxTokenConfigured: true))
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(BackendProvider(backend: IdleBackend()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(SocketProvider(socket: FakeSocket()))
            .environmentObject(VehiclesFetcher(socket: FakeSocket()))
            .environmentObject(ViewportProvider())

        XCTAssertNotNil(try sut.inspect().find(HomeMapView.self))
    }

    class FakeContentVM: ContentViewModel {
        let loadConfigCallback: () -> Void
        let loadSettingsCallback: () -> Void

        init(
            dynamicMapKeyEnabled: Bool = false,
            mapboxTokenConfigured: Bool = false,
            loadConfigCallback: @escaping () -> Void = {},
            loadSettingsCallback: @escaping () -> Void = {}
        ) {
            self.loadConfigCallback = loadConfigCallback
            self.loadSettingsCallback = loadSettingsCallback
            super.init(dynamicMapKeyEnabled: dynamicMapKeyEnabled, mapboxTokenConfigured: mapboxTokenConfigured)
        }

        override func loadConfig() async { loadConfigCallback() }

        override func loadSettings() async {
            loadSettingsCallback()
        }
    }

    class FakeSocket: MockSocket {
        let connectedExpecation: XCTestExpectation?
        let disconnectedExpectation: XCTestExpectation?

        init(connectedExpectation: XCTestExpectation? = nil, disconnectedExpectation: XCTestExpectation? = nil) {
            connectedExpecation = connectedExpectation
            self.disconnectedExpectation = disconnectedExpectation
            super.init()
        }

        override func attach() {
            connectedExpecation?.fulfill()
        }

        override func detach() {
            disconnectedExpectation?.fulfill()
        }
    }
}

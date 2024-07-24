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

        let sut = withDefaultEnvironmentObjects(sut: ContentView(contentVM: .init()),
                                                socketProvider: SocketProvider(socket: fakeSocketWithExpectations))

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

        let sut = withDefaultEnvironmentObjects(sut: ContentView(contentVM: .init()),
                                                socketProvider: SocketProvider(socket: fakeSocketWithExpectations))

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

        ViewHosting.host(view: withDefaultEnvironmentObjects(sut: sut))

        wait(for: [configFetchedExpectation], timeout: 5)
    }

    func testSetsMapboxTokenConfigOnConfigChange() throws {
        let tokenConfigExpectation = XCTestExpectation(description: "mapbox token configured")

        let fakeVM = FakeContentVM(
            dynamicMapKeyEnabled: true,
            configMapboxCallback: { tokenConfigExpectation.fulfill() }
        )
        let sut = ContentView(contentVM: fakeVM)

        ViewHosting.host(view: withDefaultEnvironmentObjects(sut: sut))

        let newConfig: ApiResult<ConfigResponse>? = ApiResultOk(data: .init(mapboxPublicToken: "FAKE_TOKEN"))

        try sut.inspect().vStack()
            .callOnChange(newValue: newConfig)
        wait(for: [tokenConfigExpectation], timeout: 5)
    }

    func testShowsMapWithoutFetchingConfigWhenFeatureFlagDisabled() throws {
        let configNotFetchedExpectation = XCTestExpectation(description: "config not fetched")
        configNotFetchedExpectation.isInverted = true

        let fakeVM = FakeContentVM(dynamicMapKeyEnabled: false,
                                   loadConfigCallback: { configNotFetchedExpectation.fulfill() })

        let sut = withDefaultEnvironmentObjects(sut: ContentView(contentVM: fakeVM))
        XCTAssertNotNil(try sut.inspect().find(HomeMapView.self))

        wait(for: [configNotFetchedExpectation], timeout: 5)
    }

    func testShowsMapWhenFeatureFlagEnabled() throws {
        let sut = withDefaultEnvironmentObjects(sut: ContentView(contentVM: FakeContentVM(dynamicMapKeyEnabled: true)))

        XCTAssertNotNil(try sut.inspect().find(HomeMapView.self))
    }

    class FakeContentVM: ContentViewModel {
        let loadConfigCallback: () -> Void
        let configMapboxCallback: () -> Void

        init(
            dynamicMapKeyEnabled: Bool = false,
            mapboxTokenConfigured _: Bool = false,
            loadConfigCallback: @escaping () -> Void = {},
            configMapboxCallback: @escaping () -> Void = {}
        ) {
            self.loadConfigCallback = loadConfigCallback
            self.configMapboxCallback = configMapboxCallback
            super.init(dynamicMapKeyEnabled: dynamicMapKeyEnabled)
        }

        override func loadConfig() async { loadConfigCallback() }

        override func configureMapboxToken(token _: String) {
            configMapboxCallback()
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

    private func withDefaultEnvironmentObjects(
        sut: some View,
        socketProvider: SocketProvider = SocketProvider(socket: FakeSocket())
    ) -> some View {
        sut
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(BackendProvider(backend: IdleBackend()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(socketProvider)
            .environmentObject(VehiclesFetcher(socket: FakeSocket()))
            .environmentObject(ViewportProvider())
    }
}

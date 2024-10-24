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
@_spi(Experimental) import MapboxMaps
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

final class ContentViewTests: XCTestCase {
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
        let disconnectedExpectation = expectation(description: "Socket has disconnected")

        let fakeSocketWithExpectations = FakeSocket(disconnectedExpectation: disconnectedExpectation)

        let sut = withDefaultEnvironmentObjects(sut: ContentView(contentVM: .init()),
                                                socketProvider: SocketProvider(socket: fakeSocketWithExpectations))

        ViewHosting.host(view: sut)

        try sut.inspect().implicitAnyView().view(ContentView.self).implicitAnyView().implicitAnyView().implicitAnyView()
            .vStack().callOnChange(newValue: ScenePhase.background)
        wait(for: [disconnectedExpectation], timeout: 5)
    }

    func testReconnectsSocketAfterBackgroundingAndReactivating() throws {
        let disconnectedExpectation = expectation(description: "Socket has disconnected")
        let connectedExpectation = expectation(description: "Socket has connected")
        connectedExpectation.expectedFulfillmentCount = 1
        connectedExpectation.assertForOverFulfill = true

        let fakeSocketWithExpectations = FakeSocket(connectedExpectation: connectedExpectation,
                                                    disconnectedExpectation: disconnectedExpectation)

        let sut = withDefaultEnvironmentObjects(sut: ContentView(contentVM: .init()),
                                                socketProvider: SocketProvider(socket: fakeSocketWithExpectations))

        ViewHosting.host(view: sut)

        try sut.inspect().implicitAnyView().view(ContentView.self).implicitAnyView().implicitAnyView().implicitAnyView()
            .vStack().callOnChange(newValue: ScenePhase.background)
        wait(for: [disconnectedExpectation], timeout: 1)
        try sut.inspect().implicitAnyView().view(ContentView.self).implicitAnyView().implicitAnyView().implicitAnyView()
            .vStack().callOnChange(newValue: ScenePhase.active)
        wait(for: [connectedExpectation], timeout: 1)
    }

    func testJoinsAlertsOnActive() throws {
        let joinAlertsExp = expectation(description: "Alerts channel joined")
        // joins in onAppear & on active
        joinAlertsExp.expectedFulfillmentCount = 1
        joinAlertsExp.assertForOverFulfill = true

        let fakeNearbyVM: NearbyViewModel = .init(alertsRepository: CallbackAlertsRepository(connectExp: joinAlertsExp))

        let sut = withDefaultEnvironmentObjects(sut: ContentView(contentVM: .init(), nearbyVM: fakeNearbyVM),
                                                socketProvider: SocketProvider(socket: MockSocket()))

        ViewHosting.host(view: sut)

        try sut.inspect().implicitAnyView().view(ContentView.self).implicitAnyView().implicitAnyView().implicitAnyView()
            .vStack().callOnChange(newValue: ScenePhase.active)
        wait(for: [joinAlertsExp], timeout: 5)
    }

    func testLeavesAlertsAfterBackgrounding() throws {
        let leavesAlertsExp = expectation(description: "Alerts channel left")

        let fakeNearbyVM: NearbyViewModel = .init(alertsRepository:
            CallbackAlertsRepository(disconnectExp: leavesAlertsExp))

        let sut = withDefaultEnvironmentObjects(sut: ContentView(contentVM: .init(), nearbyVM: fakeNearbyVM),
                                                socketProvider: SocketProvider(socket: MockSocket()))

        ViewHosting.host(view: sut)

        try sut.inspect().implicitAnyView().view(ContentView.self).implicitAnyView().implicitAnyView().implicitAnyView()
            .vStack().callOnChange(newValue: ScenePhase.background)
        wait(for: [leavesAlertsExp], timeout: 5)
    }

    func testFetchesConfig() throws {
        let configFetchedExpectation = XCTestExpectation(description: "config fetched")

        let fakeVM = FakeContentVM(
            loadConfigCallback: { configFetchedExpectation.fulfill() }
        )
        let sut = ContentView(contentVM: fakeVM)

        ViewHosting.host(view: withDefaultEnvironmentObjects(sut: sut))

        wait(for: [configFetchedExpectation], timeout: 5)
    }

    func testSetsMapboxTokenConfigOnConfigChange() throws {
        let tokenConfigExpectation = XCTestExpectation(description: "mapbox token configured")

        let fakeVM = FakeContentVM(
            configMapboxCallback: { tokenConfigExpectation.fulfill() }
        )
        let sut = withDefaultEnvironmentObjects(sut: ContentView(contentVM: fakeVM))

        ViewHosting.host(view: sut)

        let newConfig: ApiResult<ConfigResponse>? = ApiResultOk(data: .init(mapboxPublicToken: "FAKE_TOKEN"))

        try sut.inspect().implicitAnyView().view(ContentView.self).implicitAnyView().implicitAnyView().implicitAnyView()
            .vStack()
            .callOnChange(newValue: newConfig)
        wait(for: [tokenConfigExpectation], timeout: 5)
    }

    @MainActor func testFetchesConfigOnMapboxError() throws {
        let loadConfigCallback = XCTestExpectation(description: "load config called")
        loadConfigCallback.expectedFulfillmentCount = 2

        let fakeVM = FakeContentVM(
            loadConfigCallback: { loadConfigCallback.fulfill() }
        )
        let sut = ContentView(contentVM: fakeVM)

        let newConfig: ApiResult<ConfigResponse>? = ApiResultOk(data: .init(mapboxPublicToken: "FAKE_TOKEN"))

        let hasAppeared = sut.inspection.inspect(after: 2) { view in

            try view.actualView().mapVM.lastMapboxErrorSubject.send(Date.now)
        }

        ViewHosting.host(view: withDefaultEnvironmentObjects(sut: sut))

        wait(for: [hasAppeared, loadConfigCallback], timeout: 5)
    }

    @MainActor func testHidesMap() throws {
        let contentVM = FakeContentVM()
        contentVM.hideMaps = true
        let sut = ContentView(contentVM: contentVM)

        XCTAssertThrowsError(try sut.inspect().find(HomeMapView.self))
    }

    func testShowsMap() throws {
        let sut = withDefaultEnvironmentObjects(sut: ContentView(contentVM: FakeContentVM()))

        XCTAssertNotNil(try sut.inspect().find(HomeMapView.self))
    }

    class FakeContentVM: ContentViewModel {
        let loadConfigCallback: () -> Void
        let configMapboxCallback: () -> Void

        init(
            mapboxTokenConfigured _: Bool = false,
            loadConfigCallback: @escaping () -> Void = {},
            configMapboxCallback: @escaping () -> Void = {}
        ) {
            self.loadConfigCallback = loadConfigCallback
            self.configMapboxCallback = configMapboxCallback
            super.init()
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

    class CallbackAlertsRepository: IAlertsRepository {
        var connectExp: XCTestExpectation?
        var disconnectExp: XCTestExpectation?
        init(connectExp: XCTestExpectation? = nil, disconnectExp: XCTestExpectation? = nil) {
            self.connectExp = connectExp
            self.disconnectExp = disconnectExp
        }

        func connect(onReceive _: @escaping (ApiResult<AlertsStreamDataResponse>) -> Void) {
            connectExp?.fulfill()
        }

        func disconnect() {
            disconnectExp?.fulfill()
        }
    }

    private func withDefaultEnvironmentObjects(
        sut: some View,
        socketProvider: SocketProvider = SocketProvider(socket: FakeSocket())
    ) -> some View {
        sut
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(socketProvider)
            .environmentObject(ViewportProvider())
    }
}

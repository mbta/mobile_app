//
//  ContentViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 12/28/23.
//  Copyright © 2023 orgName. All rights reserved.
//

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
    }

    func testDisconnectsSocketAfterBackgrounding() throws {
        let connectedExpectation = expectation(description: "Socket has connected")
        let disconnectedExpectation = expectation(description: "Socket has disconnected")

        let fakeSocketWithExpectations = FakeSocket(connectedExpectation: connectedExpectation,
                                                    disconnectedExpectation: disconnectedExpectation)

        let sut = ContentView()
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(AlertsFetcher(socket: FakeSocket()))
            .environmentObject(GlobalFetcher(backend: IdleBackend()))
            .environmentObject(NearbyFetcher(backend: IdleBackend()))
            .environmentObject(PredictionsFetcher(socket: FakeSocket()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(ScheduleFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(SocketProvider(socket: fakeSocketWithExpectations))
            .environmentObject(ViewportProvider())

        ViewHosting.host(view: sut)

        wait(for: [connectedExpectation], timeout: 1)

        try sut.inspect().navigationView().callOnChange(newValue: ScenePhase.background)
        wait(for: [disconnectedExpectation], timeout: 1)
    }

    func testReconnectsSocketAfterBackgroundingAndReactivating() throws {
        let disconnectedExpectation = expectation(description: "Socket has disconnected")
        let connectedExpectation = expectation(description: "Socket has connected")
        connectedExpectation.expectedFulfillmentCount = 2
        connectedExpectation.assertForOverFulfill = true

        let fakeSocketWithExpectations = FakeSocket(connectedExpectation: connectedExpectation,
                                                    disconnectedExpectation: disconnectedExpectation)

        let sut = ContentView()
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(AlertsFetcher(socket: FakeSocket()))
            .environmentObject(GlobalFetcher(backend: IdleBackend()))
            .environmentObject(NearbyFetcher(backend: IdleBackend()))
            .environmentObject(PredictionsFetcher(socket: FakeSocket()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(ScheduleFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(SocketProvider(socket: fakeSocketWithExpectations))
            .environmentObject(ViewportProvider())

        ViewHosting.host(view: sut)

        try sut.inspect().navigationView().callOnChange(newValue: ScenePhase.background)
        wait(for: [disconnectedExpectation], timeout: 1)
        try sut.inspect().navigationView().callOnChange(newValue: ScenePhase.active)
        wait(for: [connectedExpectation], timeout: 1)
    }

    func testFetchesGlobalData() throws {
        struct FakeGlobalFetcherBackend: BackendProtocol {
            let expectation: XCTestExpectation
            let idle = IdleBackend()

            func getGlobalData() async throws -> GlobalResponse {
                expectation.fulfill()
                throw NotUnderTestError()
            }

            func getNearby(latitude _: Double, longitude _: Double) async throws -> NearbyResponse {
                throw NotUnderTestError()
            }

            func getSchedule(stopIds _: [String]) async throws -> ScheduleResponse {
                throw NotUnderTestError()
            }

            func getSearchResults(query _: String) async throws -> SearchResponse {
                throw NotUnderTestError()
            }

            func getRailRouteShapes() async throws -> RouteResponse {
                throw NotUnderTestError()
            }
        }

        let fetchesGlobalData = expectation(description: "fetches global data")

        let sut = ContentView()
            .environmentObject(LocationDataManager(locationFetcher: MockLocationFetcher()))
            .environmentObject(AlertsFetcher(socket: FakeSocket()))
            .environmentObject(GlobalFetcher(backend: FakeGlobalFetcherBackend(expectation: fetchesGlobalData)))
            .environmentObject(NearbyFetcher(backend: IdleBackend()))
            .environmentObject(PredictionsFetcher(socket: FakeSocket()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(ScheduleFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(SocketProvider(socket: FakeSocket()))
            .environmentObject(ViewportProvider())

        ViewHosting.host(view: sut)
        wait(for: [fetchesGlobalData], timeout: 1)
    }

    class FakeSocket: MockSocket {
        let connectedExpecation: XCTestExpectation?
        let disconnectedExpectation: XCTestExpectation?

        init(connectedExpectation: XCTestExpectation? = nil, disconnectedExpectation: XCTestExpectation? = nil) {
            connectedExpecation = connectedExpectation
            self.disconnectedExpectation = disconnectedExpectation
            super.init()
        }

        override func connect() {
            connectedExpecation?.fulfill()
        }

        override func disconnect(code _: Socket.CloseCode,
                                 reason _: String?,
                                 callback _: (() -> Void)?)
        {
            disconnectedExpectation?.fulfill()
        }
    }
}

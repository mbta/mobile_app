//
//  MainViewTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
import SwiftPhoenixClient
import SwiftUI
import ViewInspector
import XCTest

final class MainViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testDisconnectsSocketAfterBackgrounding() throws {
        class FakeSocket: MockSocket {
            let connectedExpecation: XCTestExpectation
            let disconnectedExpectation: XCTestExpectation

            init(connectedExpectation: XCTestExpectation, disconnectedExpectation: XCTestExpectation) {
                connectedExpecation = connectedExpectation
                self.disconnectedExpectation = disconnectedExpectation
                super.init()
            }

            override func connect() {
                connectedExpecation.fulfill()
            }

            override func disconnect(code _: Socket.CloseCode,
                                     reason _: String?,
                                     callback _: (() -> Void)?)
            {
                disconnectedExpectation.fulfill()
            }
        }
        var connectedExpectation = expectation(description: "Socket has connected")
        var disconnectedExpectation = expectation(description: "Socket has disconnected")

        var sut = MainView(socket: FakeSocket(connectedExpectation: connectedExpectation, disconnectedExpectation: disconnectedExpectation), backend: IdleBackend())

        ViewHosting.host(view: sut)

        wait(for: [connectedExpectation], timeout: 1)

        try sut.inspect().find(ContentView.self).callOnChange(newValue: ScenePhase.background)
        wait(for: [disconnectedExpectation], timeout: 1)
    }

    func testReconnectsSocketAfterBackgroundingAndReactivating() throws {
        class FakeSocket: MockSocket {
            let connectedExpecation: XCTestExpectation
            let disconnectedExpectation: XCTestExpectation

            init(connectedExpectation: XCTestExpectation, disconnectedExpectation: XCTestExpectation) {
                connectedExpecation = connectedExpectation
                self.disconnectedExpectation = disconnectedExpectation
                super.init()
            }

            override func connect() {
                connectedExpecation.fulfill()
            }

            override func disconnect(code _: Socket.CloseCode,
                                     reason _: String?,
                                     callback _: (() -> Void)?)
            {
                disconnectedExpectation.fulfill()
            }
        }
        let connectedExpectation = expectation(description: "Socket has connected")
        connectedExpectation.expectedFulfillmentCount = 2
        connectedExpectation.assertForOverFulfill = true
        let disconnectedExpectation = expectation(description: "Socket has disconnected")

        let sut = MainView(socket: FakeSocket(connectedExpectation: connectedExpectation, disconnectedExpectation: disconnectedExpectation), backend: IdleBackend())

        ViewHosting.host(view: sut)

        try sut.inspect().find(ContentView.self).callOnChange(newValue: ScenePhase.background)
        wait(for: [disconnectedExpectation], timeout: 1)

        try sut.inspect().find(ContentView.self).callOnChange(newValue: ScenePhase.active)
        wait(for: [connectedExpectation], timeout: 1)
    }
}

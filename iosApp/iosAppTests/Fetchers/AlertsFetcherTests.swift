//
//  AlertsFetcherTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-03-15.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
@testable import SwiftPhoenixClient
import XCTest

final class AlertsFetcherTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testSocketConnectCalledOnRun() {
        class FakeSocket: MockSocket {
            let connectionExpectation: XCTestExpectation

            init(connectionExpectation: XCTestExpectation) {
                self.connectionExpectation = connectionExpectation
            }

            override func connect() {
                connectionExpectation.fulfill()
            }
        }

        let connectionExpectation = XCTestExpectation(description: "Socket connected")

        let mockSocket = FakeSocket(connectionExpectation: connectionExpectation)
        let alertsFetcher = AlertsFetcher(socket: mockSocket)

        alertsFetcher.run()
        wait(for: [connectionExpectation])
    }

    func testChannelSetOnRun() {
        let mockSocket = MockSocket()
        let alertsFetcher = AlertsFetcher(socket: mockSocket)
        XCTAssertNil(alertsFetcher.channel)

        alertsFetcher.run()
        XCTAssertNotNil(alertsFetcher.channel)
    }

    func testChannelClearedOnLeave() {
        let mockSocket = MockSocket()
        let alertsFetcher = AlertsFetcher(socket: mockSocket)
        alertsFetcher.channel = mockSocket.channel(AlertsChannel.shared.topic, params: [:])
        XCTAssertNotNil(alertsFetcher.channel)

        alertsFetcher.leave()
        XCTAssertNil(alertsFetcher.channel)
    }

    func testSetsAlertsWhenMessageReceived() {
        let mockSocket = MockSocket()
        let messageSuccessExpectation = expectation(description: "parsed prediction payload prediction payload")

        let alertsFetcher = AlertsFetcher(socket: mockSocket, onMessageSuccessCallback: {
            messageSuccessExpectation.fulfill()
        })

        XCTAssertEqual(alertsFetcher.data, nil)
        alertsFetcher.run()

        alertsFetcher.channel!
            .trigger(Message(ref: "1",
                             topic: AlertsChannel.shared.topic,
                             event: AlertsChannel.shared.newDataEvent,
                             payload: ["jsonPayload": "{\"alerts\": {}}"],
                             joinRef: "2"))

        wait(for: [messageSuccessExpectation], timeout: 1)

        XCTAssertEqual(alertsFetcher.data, AlertsStreamDataResponse(alerts: [:]))
    }

    func testSetsErrorWhenErrorReceived() {
        let mockSocket = MockSocket()
        let errorExpectation = expectation(description: "onError callback called")

        let alertsFetcher = AlertsFetcher(socket: mockSocket, onErrorCallback: {
            errorExpectation.fulfill()
        })
        alertsFetcher.run()

        XCTAssertNil(alertsFetcher.socketError)

        alertsFetcher.channel!.trigger(event: ChannelEvent.error)
        wait(for: [errorExpectation], timeout: 1)
        XCTAssertNotNil(alertsFetcher.socketError)
    }

    func testClearsErrorMessageWhenNewAlertsReceived() {
        let mockSocket = MockSocket()
        let messageSuccessExpectation = expectation(description: "parsed prediction payload prediction payload")

        let alertsFetcher = AlertsFetcher(socket: mockSocket, onMessageSuccessCallback: {
            messageSuccessExpectation.fulfill()
        })

        alertsFetcher.socketError = PhoenixChannelError.channelError("Old Error")

        XCTAssertEqual(alertsFetcher.data, nil)
        alertsFetcher.run()

        alertsFetcher.channel!
            .trigger(Message(ref: "1",
                             topic: AlertsChannel.shared.topic,
                             event: AlertsChannel.shared.newDataEvent,
                             payload: ["jsonPayload": "{\"alerts\": {}, \"trips\": {}, \"vehicles\": {}}"],
                             joinRef: "2"))

        wait(for: [messageSuccessExpectation], timeout: 1)

        XCTAssertNil(alertsFetcher.socketError)
    }
}

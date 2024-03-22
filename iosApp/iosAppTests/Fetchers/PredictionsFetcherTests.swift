//
//  PredictionsFetcherTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 3/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import shared
@testable import SwiftPhoenixClient
import XCTest

final class PredictionsFetcherTests: XCTestCase {
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
        let predictionsFetcher = PredictionsFetcher(socket: mockSocket)

        predictionsFetcher.run(stopIds: ["1"])
        wait(for: [connectionExpectation])
    }

    func testChannelSetOnRun() {
        let mockSocket = MockSocket()
        let predictionsFetcher = PredictionsFetcher(socket: mockSocket)
        XCTAssertNil(predictionsFetcher.channel)

        predictionsFetcher.run(stopIds: ["1"])
        XCTAssertNotNil(predictionsFetcher.channel)
    }

    func testChannelClearedOnLeave() {
        let mockSocket = MockSocket()
        let predictionsFetcher = PredictionsFetcher(socket: mockSocket)
        predictionsFetcher.channel = mockSocket.channel(PredictionsForStopsChannel.companion.topic, params: [:])
        XCTAssertNotNil(predictionsFetcher.channel)

        predictionsFetcher.leave()
        XCTAssertNil(predictionsFetcher.channel)
    }

    func testSetsPredictionsOnJoinResponse() {
        let mockSocket = MockSocket()
        let messageSuccessExpectation = expectation(description: "got initial payload")

        let predictionsFetcher = PredictionsFetcher(socket: mockSocket, onMessageSuccessCallback: {
            messageSuccessExpectation.fulfill()
        })

        XCTAssertNil(predictionsFetcher.predictions)
        predictionsFetcher.run(stopIds: ["1"])

        predictionsFetcher.channel!.joinPush.trigger("ok", payload: ["jsonPayload": "{\"predictions\": {}, \"trips\": {}, \"vehicles\": {}}"])

        wait(for: [messageSuccessExpectation], timeout: 1)

        XCTAssertEqual(predictionsFetcher.predictions, .init(predictions: [:], trips: [:], vehicles: [:]))
    }

    func testSetsPredictionsWhenMessageReceived() {
        let mockSocket = MockSocket()
        let messageSuccessExpectation = expectation(description: "parsed prediction payload prediction payload")

        let predictionsFetcher = PredictionsFetcher(socket: mockSocket, onMessageSuccessCallback: {
            messageSuccessExpectation.fulfill()
        })

        XCTAssertEqual(predictionsFetcher.predictions, nil)
        predictionsFetcher.run(stopIds: ["1"])

        predictionsFetcher.channel!
            .trigger(Message(ref: "1",
                             topic: PredictionsForStopsChannel.companion.topic,
                             event: PredictionsForStopsChannel.companion.newDataEvent,
                             payload: ["jsonPayload": "{\"predictions\": {}, \"trips\": {}, \"vehicles\": {}}"],
                             joinRef: "2"))

        wait(for: [messageSuccessExpectation], timeout: 1)

        XCTAssertEqual(predictionsFetcher.predictions, PredictionsStreamDataResponse(predictions: [:], trips: [:], vehicles: [:]))
    }

    func testSetsErrorWhenErrorReceived() {
        let mockSocket = MockSocket()
        let errorExpectation = expectation(description: "onError callback called")

        let predictionsFetcher = PredictionsFetcher(socket: mockSocket, onErrorCallback: {
            errorExpectation.fulfill()
        })
        predictionsFetcher.run(stopIds: ["1"])

        XCTAssertNil(predictionsFetcher.socketError)

        predictionsFetcher.channel!.trigger(event: ChannelEvent.error)
        wait(for: [errorExpectation], timeout: 1)
        XCTAssertNotNil(predictionsFetcher.socketError)
    }

    func testClearsErrorMessageWhenNewPredictionsReceived() {
        let mockSocket = MockSocket()
        let messageSuccessExpectation = expectation(description: "parsed prediction payload prediction payload")

        let predictionsFetcher = PredictionsFetcher(socket: mockSocket, onMessageSuccessCallback: {
            messageSuccessExpectation.fulfill()
        })

        predictionsFetcher.socketError = PhoenixChannelError.channelError("Old Error")

        XCTAssertEqual(predictionsFetcher.predictions, nil)
        predictionsFetcher.run(stopIds: ["1"])

        predictionsFetcher.channel!
            .trigger(Message(ref: "1",
                             topic: PredictionsForStopsChannel.companion.topic,
                             event: PredictionsForStopsChannel.companion.newDataEvent,
                             payload: ["jsonPayload": "{\"predictions\": {}, \"trips\": {}, \"vehicles\": {}}"],
                             joinRef: "2"))

        wait(for: [messageSuccessExpectation], timeout: 1)

        XCTAssertNil(predictionsFetcher.socketError)
    }
}

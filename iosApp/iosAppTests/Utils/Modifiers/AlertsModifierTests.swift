//
//  AlertsModifierTests.swift
//  iosApp
//
//  Created by esimon on 6/22/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class AlertsModifierTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testLoadsFromRepo() {
        let repoExp = expectation(description: "alerts loaded from repo")
        let setExp = expectation(description: "alerts binding was set")
        setExp.assertForOverFulfill = false

        let updatedAlerts = AlertsStreamDataResponse(alerts: [:])
        let alertBinding: Binding<AlertsStreamDataResponse?> = .init(get: { nil }, set: {
            XCTAssertEqual(updatedAlerts, $0)
            setExp.fulfill()
        })
        var repoFulfilled = false
        let mockRepos = MockRepositories()
        mockRepos.alerts = MockAlertsRepository(
            response: .init(remove: [], update: [:]),
            onConnect: {
                guard !repoFulfilled else { return }
                repoFulfilled = true
                repoExp.fulfill()
            }
        )
        loadKoinMocks(repositories: mockRepos)

        let sut = Text("test").alerts(alertBinding)
        ViewHosting.host(view: sut)
        wait(for: [repoExp, setExp], timeout: 1)
    }
}

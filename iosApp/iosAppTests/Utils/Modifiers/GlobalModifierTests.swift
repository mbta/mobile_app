//
//  GlobalModifierTests.swift
//  iosApp
//
//  Created by esimon on 9/5/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class GlobalModifierTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testLoadsFromRepo() throws {
        let repoExp = expectation(description: "favorites loaded from repo")
        let setExp = expectation(description: "favorites binding was set")

        let updatedGlobal = GlobalResponse(objects: .init())

        let globalBinding: Binding<GlobalResponse?> = .init(get: { nil }, set: {
            XCTAssertEqual(updatedGlobal, $0)
            setExp.fulfill()
        })

        var repoFulfilled = false
        let mockRepos = MockRepositories()
        mockRepos.global = MockGlobalRepository(
            response: updatedGlobal,
            onGet: {
                guard !repoFulfilled else { return }
                repoFulfilled = true
                repoExp.fulfill()
            }
        )
        loadKoinMocks(repositories: mockRepos)

        let sut = Text("test").global(globalBinding, errorKey: "ErrorKey")

        ViewHosting.host(view: sut)

        wait(for: [repoExp, setExp], timeout: 1)
    }
}

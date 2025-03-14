//
//  ScenePhaseChangeModifierTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 5/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class ScenePhaseChangeModifierTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testOnActiveCalled() throws {
        let activeCalledExpectation = XCTestExpectation(description: "onActive called")

        let sut = Text("hello").withScenePhaseHandlers(onActive: { activeCalledExpectation.fulfill() },
                                                       onInactive: {},
                                                       onBackground: {})

        ViewHosting.host(view: sut)

        try sut.inspect().find(ViewType.Text.self).modifier(ScenePhaseChangeModifier.self).implicitAnyView()
            .viewModifierContent().callOnChange(newValue: ScenePhase.active)

        wait(for: [activeCalledExpectation], timeout: 1)
    }

    func testOnInactiveCalled() throws {
        let inactiveCalledExpectation = XCTestExpectation(description: "onInactive called")

        let sut = Text("hello").withScenePhaseHandlers(onActive: {},
                                                       onInactive: { inactiveCalledExpectation.fulfill() },
                                                       onBackground: {})

        ViewHosting.host(view: sut)

        try sut.inspect().find(ViewType.Text.self).modifier(ScenePhaseChangeModifier.self).implicitAnyView()
            .viewModifierContent().callOnChange(newValue: ScenePhase.inactive)

        wait(for: [inactiveCalledExpectation], timeout: 1)
    }

    func testOnBackgroundCalled() throws {
        let backgroundCalledExpectation = XCTestExpectation(description: "onBackground called")

        let sut = Text("hello").withScenePhaseHandlers(onActive: {},
                                                       onInactive: {},
                                                       onBackground: { backgroundCalledExpectation.fulfill() })

        ViewHosting.host(view: sut)

        try sut.inspect().find(ViewType.Text.self).modifier(ScenePhaseChangeModifier.self).implicitAnyView()
            .viewModifierContent().callOnChange(newValue: ScenePhase.background)

        wait(for: [backgroundCalledExpectation], timeout: 1)
    }
}

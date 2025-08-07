//
//  ToastViewTests.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/28/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class ToastViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testTextOnly() throws {
        let textOnly = ToastState(
            message: "This is a text only toast",
            duration: ToastViewModel.Duration.indefinite,
            onClose: nil,
            actionLabel: nil,
            onAction: nil,
        )

        let sut = ToastView(state: textOnly, tabBarVisible: false, onDismiss: {})

        XCTAssertThrowsError(try sut.inspect().find(viewWithAccessibilityLabel: "Close"))
        XCTAssertNotNil(try sut.inspect().find(text: "This is a text only toast"))
    }

    func testClose() throws {
        let close = ToastState(
            message: "This is a toast with a close button",
            duration: ToastViewModel.Duration.indefinite,
            onClose: {},
            actionLabel: nil,
            onAction: nil,
        )

        let exp = XCTestExpectation(description: "Close button pressed")
        let sut = ToastView(state: close, tabBarVisible: false, onDismiss: { exp.fulfill() })

        try sut.inspect().find(viewWithAccessibilityLabel: "Close").button().tap()
        XCTAssertNotNil(try sut.inspect().find(text: "This is a toast with a close button"))
        wait(for: [exp], timeout: 1)
    }

    func testAction() throws {
        let action = ToastState(
            message: "This is a toast with an action button",
            duration: ToastViewModel.Duration.indefinite,
            onClose: nil,
            actionLabel: "Action",
            onAction: {},
        )

        let exp = XCTestExpectation(description: "Action button pressed")
        let sut = ToastView(state: action, tabBarVisible: false, onDismiss: { exp.fulfill() })

        try sut.inspect().find(button: "Action").tap()
        XCTAssertNotNil(try sut.inspect().find(text: "This is a toast with an action button"))
        wait(for: [exp], timeout: 1)
    }
}

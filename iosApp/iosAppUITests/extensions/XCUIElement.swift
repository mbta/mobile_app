//
//  XCUIElement.swift
//  iosApp
//
//  Created by Kayla Brady on 11/12/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import XCTest

extension XCUIElement {
    /// Waits for the element to exist and taps it. Fails the test if it doesn't appear.
    func tapAfter(
        timeout: TimeInterval = 10.0,
        file: StaticString = #file,
        line: UInt = #line,
        beforeTapAction: () -> Void = {}
    ) {
        if waitForExistence(timeout: timeout) {
            beforeTapAction()
            tap()
        } else {
            XCTFail("Element \(description) did not appear within \(timeout) seconds.", file: file, line: line)
        }
    }
}

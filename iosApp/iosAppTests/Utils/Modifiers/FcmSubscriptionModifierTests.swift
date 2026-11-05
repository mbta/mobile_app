//
//  FcmSubscriptionModifierTests.swift
//  iosApp
//
//  Created by esimon on 12/1/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class FcmSubscriptionModifierTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testSetsLocaleAndFcmToken() {
        let localeExp = expectation(description: "locale set")
        let tokenExp = expectation(description: "fcm token set")

        let expectedToken = "token_string"
        let favoritesVM = MockFavoritesViewModel()
        favoritesVM.onSetLocale = { locale in
            XCTAssertEqual("en", locale)
            localeExp.fulfill()
        }
        favoritesVM.onSetFcmToken = { token in
            XCTAssertEqual(expectedToken, token)
            tokenExp.fulfill()
        }

        let sut = Text("test").handleFcmTokenSubscriptions(
            fcmToken: expectedToken,
            favoritesVM: favoritesVM
        )

        ViewHosting.host(view: sut)
        defer { ViewHosting.expel() }

        wait(for: [localeExp, tokenExp], timeout: 1)
    }
}

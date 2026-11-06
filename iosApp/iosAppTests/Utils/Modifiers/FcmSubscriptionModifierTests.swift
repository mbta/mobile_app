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
        let installationIdExp = expectation(description: "fcm installation id set")

        let expectedInstallationId = "installation_id_string"
        let favoritesVM = MockFavoritesViewModel()
        favoritesVM.onSetLocale = { locale in
            XCTAssertEqual("en", locale)
            localeExp.fulfill()
        }
        favoritesVM.onSetFcmInstallationId = { installationId in
            XCTAssertEqual(expectedInstallationId, installationId)
            installationIdExp.fulfill()
        }

        let sut = Text("test").handleFcmInstallationIdSubscriptions(
            fcmInstallationId: expectedInstallationId,
            favoritesVM: favoritesVM
        )

        ViewHosting.host(view: sut)
        defer { ViewHosting.expel() }

        wait(for: [localeExp, installationIdExp], timeout: 1)
    }
}

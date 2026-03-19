//
//  NotificationsBetaModifierTests.swift
//  iosApp
//
//  Created by esimon on 3/19/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Foundation
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class NotificationsBetaModifierTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor
    func testDisplaysToast() throws {
        let toastDisplayedExp = expectation(description: "toast displayed")

        let viewModel = MockNotificationsBetaViewModel(initialState: .init(showBetaToast: true, showBetaDialog: false))

        let toastViewModel = MockToastViewModel()
        toastViewModel.onShowToast = { toast in
            XCTAssertEqual("notifications_beta_toast_message_key", toast.message)
            toastDisplayedExp.fulfill()
        }
        let sut = Text("Test").notificationsBeta(
            navEntry: .favorites,
            onToastTap: {},
            onDismissDialog: {},
            instanceIdCache: MockInstanceIdCache(instanceId: ""),
            viewModel: viewModel,
            toastViewModel: toastViewModel,
        )

        ViewHosting.host(view: sut.withFixedSettings([.notifications: true]))
        wait(for: [toastDisplayedExp], timeout: 1)
    }

    @MainActor
    func testSetsVMState() throws {
        let instanceIdSetExp = expectation(description: "set instance id")
        instanceIdSetExp.expectedFulfillmentCount = 2
        let sheetRouteSetExp = expectation(description: "set sheet route")
        let notificationEnabledSetExp = expectation(description: "set notifications enabled")

        let viewModel = MockNotificationsBetaViewModel()
        viewModel.onSetInstanceId = { _ in instanceIdSetExp.fulfill() }
        viewModel.onSetSheetRoute = { _ in sheetRouteSetExp.fulfill() }
        viewModel.onSetNotificationsEnabled = { _ in notificationEnabledSetExp.fulfill() }

        let instanceIdCache = MockInstanceIdCache(instanceId: nil)

        let sut = Text("Test").notificationsBeta(
            navEntry: .favorites,
            onToastTap: {},
            onDismissDialog: {},
            instanceIdCache: instanceIdCache,
            viewModel: viewModel,
            toastViewModel: MockToastViewModel(),
        )

        ViewHosting.host(view: sut.withFixedSettings([.notifications: true]))

        instanceIdCache.instanceId = "new ID"

        wait(for: [instanceIdSetExp, sheetRouteSetExp, notificationEnabledSetExp], timeout: 1)
    }
}

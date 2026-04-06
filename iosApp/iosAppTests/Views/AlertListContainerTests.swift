//
//  AlertListContainerTests.swift
//  iosAppTests
//
//  Created by Kayla Brady on 3/24/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class AlertListContainerTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    func testShowsAllAlerts() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()

        let highAlert = objects.alert { alert in
            alert.effect = .shuttle
            alert.header = "Test header"
        }

        let highAlertSummary = AlertSummary.Standard(effect: .shuttle,
                                                     location: .some(AlertSummary.LocationSuccessiveStops(
                                                         startStopName: "Start Stop",
                                                         endStopName: "End Stop"
                                                     )),
                                                     timeframe: .some(AlertSummary.TimeframeTomorrow()),
                                                     recurrence: nil,
                                                     isUpdate: false)

        let downstreamAlert = objects.alert { alert in
            alert.effect = .serviceChange
            alert.activePeriod(
                start: now.minus(hours: 3 * 24),
                end: now.plus(hours: 3 * 24)
            )
        }

        let sut = AlertListContainer(displayAlerts: .init(highPriority: [.init(alert: highAlert, isDownstream: false)],
                                                          lowPriority: [.init(
                                                              alert: downstreamAlert,
                                                              isDownstream: true
                                                          )]),
                                     showNotAccessibleCard: true,
                                     alertSummaries: [highAlert.id: highAlertSummary, downstreamAlert.id: nil],
                                     now: now,
                                     isAllServiceDisrupted: false,
                                     routeAccents: .init(),
                                     onRowTap: { _, _ in })

        XCTAssertNotNil(try sut.inspect().find(text: "Shuttle buses from Start Stop to End Stop through tomorrow"))
        XCTAssertNotNil(try sut.inspect().find(text: "This stop is not accessible"))
        XCTAssertNotNil(try sut.inspect().find(text: "Service change ahead"))
    }

    func testNoAlertsByDefault() throws {
        let now = EasternTimeInstant.now()
        let objects = ObjectCollectionBuilder()

        let sut = AlertListContainer(displayAlerts: .init(highPriority: [],
                                                          lowPriority: []),
                                     showNotAccessibleCard: false,
                                     alertSummaries: [:],
                                     now: now,
                                     isAllServiceDisrupted: false,
                                     routeAccents: .init(),
                                     onRowTap: { _, _ in })

        XCTAssertThrowsError(try sut.inspect().find(text: "Shuttle buses from Start Stop to End Stop through tomorrow"))
        XCTAssertThrowsError(try sut.inspect().find(text: "This stop is not accessible"))
        XCTAssertThrowsError(try sut.inspect().find(text: "Service change ahead"))
    }
}

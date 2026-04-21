//
//  FormattedAlertTests.swift
//  iosApp
//
//  Created by esimon on 4/21/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared
import SwiftUI
import ViewInspector
import XCTest

@testable import iosApp

final class FormattedAlertTests: XCTestCase {
    func testTripShuttle() throws {
        let formatted = FormattedAlert(
            alert: nil,
            alertSummary: TripShuttleAlertSummary(
                tripIdentity: TripShuttleAlertSummary.SingleTrip(
                    tripTime: .init(
                        year: 2026,
                        month: .march,
                        day: 9,
                        hour: 12,
                        minute: 13,
                        second: 0
                    ),
                    routeType: .commuterRail,
                    fromStop: nil
                ),
                startStopName: "Oak Grove",
                endStopName: "Forest Hills",
                recurrence: nil
            )
        )
        XCTAssertEqual(
            "Shuttle buses replace the 12:13\u{202F}PM train from Oak Grove to Forest Hills",
            String(formatted.alertCardMajorBody.characters[...])
        )
    }

    func testDownstreamTripShuttle() throws {
        let formatted = FormattedAlert(
            alert: nil,
            alertSummary: TripShuttleAlertSummary(
                tripIdentity: TripShuttleAlertSummary.SingleTrip(
                    tripTime: .init(
                        year: 2026,
                        month: .march,
                        day: 9,
                        hour: 12,
                        minute: 13,
                        second: 0
                    ),
                    routeType: .commuterRail,
                    fromStop: "Oak Grove"
                ),
                startStopName: "Ruggles",
                endStopName: "Forest Hills",
                recurrence: nil
            )
        )
        XCTAssertEqual(
            "12:13\u{202F}PM train from Oak Grove is replaced by shuttle buses from Ruggles to Forest Hills",
            String(formatted.alertCardMajorBody.characters[...])
        )
    }

    func testThisTripShuttle() throws {
        let formatted = FormattedAlert(
            alert: nil,
            alertSummary: TripShuttleAlertSummary(
                tripIdentity: TripShuttleAlertSummary.ThisTrip(
                    routeType: .commuterRail
                ),
                startStopName: "Porter",
                endStopName: "North Station",
                recurrence: nil
            )
        )
        XCTAssertEqual(
            "Shuttle buses replace this train from Porter to North Station",
            String(formatted.alertCardMajorBody.characters[...])
        )
    }

    func testDownstreamTripShuttleRecurrence() throws {
        let formatted = FormattedAlert(
            alert: nil,
            alertSummary: TripShuttleAlertSummary(
                tripIdentity: TripShuttleAlertSummary.SingleTrip(
                    tripTime: .init(
                        year: 2026,
                        month: .march,
                        day: 9,
                        hour: 12,
                        minute: 13,
                        second: 0
                    ),
                    routeType: .commuterRail,
                    fromStop: "Oak Grove"
                ),
                startStopName: "Ruggles",
                endStopName: "Forest Hills",
                recurrence: AlertSummary.RecurrenceDaily(
                    ending: AlertSummary.TimeframeThisWeek(
                        time: .init(
                            year: 2026,
                            month: .march,
                            day: 12,
                            hour: 9,
                            minute: 6,
                            second: 0
                        )
                    )
                )
            )
        )
        XCTAssertEqual(
            "12:13\u{202F}PM train from Oak Grove is replaced by shuttle buses from Ruggles to Forest Hills daily until Thursday",
            String(formatted.alertCardMajorBody.characters[...])
        )
    }

    func testThisTripShuttleRecurrence() throws {
        let formatted = FormattedAlert(
            alert: nil,
            alertSummary: TripShuttleAlertSummary(
                tripIdentity: TripShuttleAlertSummary.ThisTrip(
                    routeType: .commuterRail
                ),
                startStopName: "Porter",
                endStopName: "North Station",
                recurrence: AlertSummary.RecurrenceDaily(
                    ending: AlertSummary.TimeframeThisWeek(
                        time: .init(
                            year: 2026,
                            month: .march,
                            day: 12,
                            hour: 9,
                            minute: 6,
                            second: 0
                        )
                    )
                )
            )
        )
        XCTAssertEqual(
            "Shuttle buses replace this train from Porter to North Station daily until Thursday",
            String(formatted.alertCardMajorBody.characters[...])
        )
    }

    func testTripSuspension() throws {
        let formatted = FormattedAlert(
            alert: nil,
            alertSummary: TripSpecificAlertSummary(
                tripIdentity: TripSpecificAlertSummary.TripFrom(
                    tripTime: .init(
                        year: 2026,
                        month: .march,
                        day: 9,
                        hour: 12,
                        minute: 13,
                        second: 0
                    ),
                    routeType: .commuterRail,
                    stopName: "Ruggles"
                ),
                effect: .suspension,
                effectStops: nil,
                isToday: true,
                cause: .weather
            )
        )
        XCTAssertEqual(
            "12:13\u{202F}PM train from Ruggles is suspended today due to weather",
            String(formatted.alertCardMajorBody.characters[...])
        )
    }

    func testThisTripSuspension() throws {
        let formatted = FormattedAlert(
            alert: nil,
            alertSummary: TripSpecificAlertSummary(
                tripIdentity: TripSpecificAlertSummary.ThisTrip(
                    routeType: .commuterRail
                ),
                effect: .suspension,
                effectStops: nil,
                isToday: true,
                cause: .weather
            )
        )
        XCTAssertEqual(
            "This train is suspended today due to weather",
            String(formatted.alertCardMajorBody.characters[...])
        )
    }

    func testDownstreamTripSuspension() throws {
        let formatted = FormattedAlert(
            alert: nil,
            alertSummary: TripSpecificAlertSummary(
                tripIdentity: TripSpecificAlertSummary.TripFrom(
                    tripTime: .init(
                        year: 2026,
                        month: .march,
                        day: 9,
                        hour: 12,
                        minute: 13,
                        second: 0
                    ),
                    routeType: .commuterRail,
                    stopName: "Ruggles"
                ),
                effect: .suspension,
                effectStops: ["South Station"],
                isToday: true,
                cause: .weather
            )
        )
        XCTAssertEqual(
            "12:13\u{202F}PM train from Ruggles will terminate at South Station today due to weather",
            String(formatted.alertCardMajorBody.characters[...])
        )
    }

    func testThisDownstreamTripSuspension() throws {
        let formatted = FormattedAlert(
            alert: nil,
            alertSummary: TripSpecificAlertSummary(
                tripIdentity: TripSpecificAlertSummary.ThisTrip(
                    routeType: .commuterRail
                ),
                effect: .suspension,
                effectStops: ["South Station"],
                isToday: true,
                cause: .weather
            )
        )
        XCTAssertEqual(
            "This train will terminate at South Station today due to weather",
            String(formatted.alertCardMajorBody.characters[...])
        )
    }

    func testTripStopSkipped() throws {
        let formatted = FormattedAlert(
            alert: nil,
            alertSummary: TripSpecificAlertSummary(
                tripIdentity: TripSpecificAlertSummary.TripTo(
                    tripTime: .init(
                        year: 2026,
                        month: .march,
                        day: 9,
                        hour: 12,
                        minute: 13,
                        second: 0
                    ),
                    routeType: .commuterRail,
                    headsign: "South Station"
                ),
                effect: .stationClosure,
                effectStops: ["Back Bay", "Ruggles"],
                isToday: true,
                cause: .weather
            )
        )
        XCTAssertEqual(
            "12:13\u{202F}PM train to South Station will not stop at Back Bay and Ruggles today due to weather",
            String(formatted.alertCardMajorBody.characters[...])
        )
    }

    func testThisTripStopSkipped() throws {
        let formatted = FormattedAlert(
            alert: nil,
            alertSummary: TripSpecificAlertSummary(
                tripIdentity: TripSpecificAlertSummary.ThisTrip(
                    routeType: .ferry
                ),
                effect: .dockClosure,
                effectStops: ["Rowes Wharf"],
                isToday: true,
                cause: .weather
            )
        )

        XCTAssertEqual(
            "This ferry will not stop at Rowes Wharf today due to weather",
            String(formatted.alertCardMajorBody.characters[...])
        )
    }
}

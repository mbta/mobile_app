//
//  StopAccessibilitySubheader.swift
//  iosApp
//
//  Created by Kayla Brady on 10/22/26.
//  Copyright © 2026 MBTA. All rights reserved.
//
import Shared
import SwiftUI

struct StopAccessibilitySubheader: View {
    @ObserveInjection var inject
    let stop: Stop
    let elevatorAlerts: [Shared.Alert]
    let globalData: GlobalResponse?

    @EnvironmentObject var settingsCache: SettingsCache
    var showStationAccessibility: Bool {
        settingsCache.get(.stationAccessibility) && !globalData
            .isStopBlocklisted(stop.parentStationId ?? stop.id)
    }

    var showInaccessible: Bool { showStationAccessibility && !stop.isWheelchairAccessible }
    var showElevatorAlerts: Bool { showStationAccessibility && !elevatorAlerts.isEmpty }

    var body: some View {
        if showInaccessible || showElevatorAlerts {
            HStack(spacing: 5) {
                if showElevatorAlerts {
                    Image(.accessibilityIconAlert)
                        .accessibilityHidden(true)
                } else if showInaccessible {
                    Image(.accessibilityIconNotAccessible)
                        .accessibilityHidden(true)
                        .tag("wheelchair_not_accessible")
                }
                Group {
                    if showInaccessible {
                        Text(
                            "Not accessible",
                            comment: "Header displayed when station is not wheelchair accessible"
                        )
                    } else {
                        Text(
                            "\(elevatorAlerts.count, specifier: "%ld") elevators closed",
                            comment: "Header displayed when elevators are not working at a station"
                        )
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .multilineTextAlignment(.leading)
                .font(Typography.footnoteSemibold)
                .foregroundColor(Color.accessibility)
            }.enableInjection()
        }
    }
}

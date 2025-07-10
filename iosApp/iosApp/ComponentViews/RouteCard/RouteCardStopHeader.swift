//
//  RouteCardStopHeader.swift
//  iosApp
//
//  Created by esimon on 4/11/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteCardStopHeader: View {
    let data: RouteCardData.RouteStopData

    @EnvironmentObject var settingsCache: SettingsCache
    var showStationAccessibility: Bool { settingsCache.get(.stationAccessibility) }

    var elevatorAlerts: Int { data.elevatorAlerts.count }
    var showInaccessible: Bool { showStationAccessibility && !data.stop.isWheelchairAccessible }
    var showElevatorAlerts: Bool { showStationAccessibility && data.hasElevatorAlerts }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(data.stop.name)
                .font(Typography.callout)
                .foregroundStyle(Color.text)

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
                                "\(elevatorAlerts, specifier: "%ld") elevators closed",
                                comment: "Header displayed when elevators are not working at a station"
                            )
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .multilineTextAlignment(.leading)
                    .font(Typography.footnoteSemibold)
                    .foregroundColor(Color.accessibility)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.fill2)
    }
}

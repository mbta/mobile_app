//
//  StopCardStopHeader.swift
//  iosApp
//
//  Created by Melody Horn on 6/25/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopCardStopHeader: View {
    @ObserveInjection var inject
    let data: StopCardData

    @EnvironmentObject var settingsCache: SettingsCache
    var showStationAccessibility: Bool { settingsCache.get(.stationAccessibility) }

    var elevatorAlerts: Int { data.elevatorAlerts.count }
    var showInaccessible: Bool { showStationAccessibility && !data.stop.isWheelchairAccessible }
    var showElevatorAlerts: Bool { showStationAccessibility && !data.elevatorAlerts.isEmpty }

    var body: some View {
        HStack(spacing: 4) {
            (data.stop.locationType == .stop ? Image(.mapStopCloseBUS) : Image(.mbtaLogo))
                .resizable()
                .scaledToFit()
                .frame(width: 24, height: 24)
                .accessibilityHidden(true)
                .padding(.horizontal, 8)
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
            .padding(.trailing, 16)
            .padding(.vertical, 12)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.fill2)
        .enableInjection()
    }
}

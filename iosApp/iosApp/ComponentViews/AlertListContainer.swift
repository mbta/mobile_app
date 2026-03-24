//
//  AlertListContainer.swift
//  iosApp
//
//  Created by Kayla Brady on 3/24/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct AlertListContainer: View {
    let displayAlerts: DisplayAlerts
    let showNotAccessibleCard: Bool
    let alertSummaries: [String: AlertSummary?]
    let now: EasternTimeInstant
    let isAllServiceDisrupted: Bool
    let routeAccents: TripRouteAccents
    let onRowTap: (String, AlertCardSpec) -> Void

    var body: some View {
        VStack(spacing: 1) {
            let highPriorityCount = displayAlerts.highPriority.count
            let lowPriorityCount = displayAlerts.lowPriority.count
            let outerCornerRadius: CGFloat = 8
            let internalCornerRadius: CGFloat = 4

            ForEach(Array(displayAlerts.highPriority.enumerated()), id: \.element.id) { index, displayAlert in
                let topRadius = index == 0 ? outerCornerRadius : internalCornerRadius
                let bottomRadius = index == highPriorityCount - 1 && !showNotAccessibleCard && lowPriorityCount ==
                    0 ? outerCornerRadius : internalCornerRadius

                if alertSummaries.keys.contains(displayAlert.id) {
                    let alert = displayAlert.alert
                    let spec = displayAlert.cardSpec(now: now, isAllServiceDisrupted: isAllServiceDisrupted)

                    AlertCard(
                        alert: alert,
                        alertSummary: alertSummaries[alert.id] ?? nil,
                        spec: spec,
                        routeAccents: routeAccents,
                        onViewDetails: { onRowTap(alert.id, spec) }
                    )
                    .background(Color.fill3)
                    .withUnevenRoundedBorder(topRadius: topRadius, bottomRadius: bottomRadius)
                }
            }

            if showNotAccessibleCard {
                let topRadius = highPriorityCount == 0 ? outerCornerRadius : internalCornerRadius
                let bottomRadius = lowPriorityCount == 0 ? outerCornerRadius : internalCornerRadius

                NotAccessibleCard()
                    .background(Color.fill3)
                    .withUnevenRoundedBorder(topRadius: topRadius, bottomRadius: bottomRadius, color: Color.clear)
            }
            ForEach(Array(displayAlerts.lowPriority.enumerated()), id: \.element.id) { index, displayAlert in
                let topRadius = index == 0 && highPriorityCount == 0 && !showNotAccessibleCard ? outerCornerRadius
                    : internalCornerRadius
                let bottomRadius = index == lowPriorityCount - 1 ? outerCornerRadius : internalCornerRadius

                if alertSummaries.keys.contains(displayAlert.id) {
                    let alert = displayAlert.alert
                    let spec = displayAlert.cardSpec(now: now, isAllServiceDisrupted: isAllServiceDisrupted)

                    AlertCard(
                        alert: alert,
                        alertSummary: alertSummaries[alert.id] ?? nil,
                        spec: spec,
                        routeAccents: routeAccents,
                        onViewDetails: { onRowTap(alert.id, spec) }
                    )
                    .background(Color.fill2)
                    .withUnevenRoundedBorder(topRadius: topRadius, bottomRadius: bottomRadius, color: Color.clear)
                }
            }
        }

        .padding(2)
        .background(Color.halo)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

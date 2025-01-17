//
//  AlertCard.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-15.
//  Copyright © 2025 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct AlertCard: View {
    let alert: shared.Alert
    let color: Color
    let textColor: Color
    let onViewDetails: (() -> Void)?

    @ScaledMetric var iconSize = 48

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 16) {
                AlertIcon(alertState: alert.alertState, color: color)
                    .frame(width: iconSize, height: iconSize)
                Text(FormattedAlert(alert: alert)?.effect ?? alert.effect.name)
                    .font(Typography.title2Bold)
            }
            color.opacity(0.25).frame(height: 2)
            Text(alert.header ?? "")
                .font(Typography.callout)
            if let onViewDetails {
                Button(
                    action: onViewDetails,
                    label: {
                        Text("View details", comment: "Button that shows more informaton about an alert")
                            .frame(maxWidth: .infinity)
                    }
                )
                .foregroundStyle(textColor)
                .font(Typography.bodySemibold)
                .padding(10)
                .frame(minHeight: 44)
                .background(color)
                .clipShape(.rect(cornerRadius: 8.0))
            }
        }
        .padding(16)
        .background(Color.fill3)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

#Preview {
    AlertCard(alert: ObjectCollectionBuilder.Single.shared.alert { alert in
        alert.header = "Orange Line suspended from point A to point B"
        alert.effect = .suspension
    }, color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), onViewDetails: {})
        .padding(32)
        .background(Color.fill2)
}

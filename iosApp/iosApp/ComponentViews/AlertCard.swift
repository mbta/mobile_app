//
//  AlertCard.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-15.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

enum AlertCardSpec {
    case major
    case downstream
    case secondary
    case elevator
    case delay
}

struct AlertCard: View {
    let alert: Shared.Alert
    let alertSummary: AlertSummary?
    let spec: AlertCardSpec
    let color: Color
    let textColor: Color
    let onViewDetails: (() -> Void)?

    @ScaledMetric var majorIconSize = 48
    @ScaledMetric var elevatorIconSize = 36

    @ScaledMetric var miniIconSize = 20
    @ScaledMetric var infoIconSize = 16

    var formattedAlert: FormattedAlert {
        FormattedAlert(alert: alert, alertSummary: alertSummary)
    }

    var iconSize: Double {
        switch spec {
        case .major: majorIconSize
        case .elevator: elevatorIconSize
        default: miniIconSize
        }
    }

    var headerFont: Typography {
        switch spec {
        case .major: Typography.title2Bold
        case .secondary: Typography.calloutSemibold
        default: .callout
        }
    }

    @ViewBuilder
    var card: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(alignment: .top, spacing: 16) {
                    AlertIcon(alertState: alert.alertState, color: color)
                        .scaledToFit()
                        .frame(width: iconSize, height: iconSize, alignment: .top)
                    Text(formattedAlert.alertCardHeader(spec: spec))
                        .font(headerFont)
                        .multilineTextAlignment(.leading)
                }
                if spec != .major {
                    Spacer()
                    InfoIcon(size: infoIconSize)
                }
            }
            if spec == .major {
                color.opacity(0.25).frame(height: 2)
                Text(formattedAlert.alertCardMajorBody)
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
        }
        .padding(16)
        .background(Color.fill3)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .padding(1)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.halo, lineWidth: 2))
    }

    var body: some View {
        if spec != .major, let onViewDetails {
            Button(
                action: onViewDetails,
                label: { card }
            ).foregroundStyle(Color.text)
        } else {
            card
        }
    }
}

#Preview {
    VStack {
        AlertCard(
            alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                alert.header = "Orange Line suspended from point A to point B"
                alert.effect = .suspension
            },
            // TODO: revisit previews
            alertSummary: nil,
            spec: .major,
            color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), onViewDetails: {}
        )
        .padding(32)
        .background(Color.fill2)

        AlertCard(
            alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                alert.effect = .serviceChange
            },
            alertSummary: nil,
            spec: .secondary,
            color: Color(hex: "80276C"), textColor: Color(hex: "FFFFFF"), onViewDetails: {}
        )
        .padding(32)
        .background(Color.fill2)

        AlertCard(
            alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                alert.effect = .elevatorClosure
                alert.header = "Ruggles elevator 321 (Orange Line Platform to lobby) unavailable due to maintenance"
            },
            alertSummary: nil,
            spec: .elevator,
            color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), onViewDetails: {}
        )
        .padding(32)
        .background(Color.fill2)
    }
}

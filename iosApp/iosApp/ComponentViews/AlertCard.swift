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
    let routeAccents: TripRouteAccents
    let onViewDetails: (() -> Void)?
    let internalPadding: EdgeInsets

    @ScaledMetric var majorIconSize = 48
    @ScaledMetric var elevatorIconSize = 36

    @ScaledMetric var miniIconSize = 20
    @ScaledMetric var infoIconSize = 16

    init(
        alert: Shared.Alert,
        alertSummary: AlertSummary?,
        spec: AlertCardSpec,
        routeAccents: TripRouteAccents,
        onViewDetails: (() -> Void)?,
        internalPadding: EdgeInsets = .init()
    ) {
        self.alert = alert
        self.alertSummary = alertSummary
        self.spec = spec
        self.routeAccents = routeAccents
        self.onViewDetails = onViewDetails
        self.internalPadding = internalPadding
    }

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

    @ViewBuilder
    var card: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(alignment: .center, spacing: 16) {
                    // Override alert state and icon size in the case of all clear
                    let alertState = alertSummary is AlertSummary.AllClear ? .allClear : alert.alertState
                    let iconSize = alertState == .allClear ? elevatorIconSize : iconSize
                    AlertIcon(
                        alertState: alertState,
                        color: routeAccents.color,
                        overrideIcon: alert.effect == .cancellation ? routeSlashIcon(routeAccents.type) : nil
                    )
                    .scaledToFit()
                    .frame(width: iconSize, height: iconSize, alignment: .center)
                    Text(formattedAlert.alertCardHeader(spec: spec, type: routeAccents.type))
                        .font(spec == .major ? Typography.title2Bold : Typography.callout)
                        .multilineTextAlignment(.leading)
                }
                if spec != .major {
                    Spacer()
                    InfoIcon(size: infoIconSize)
                }
            }
            if spec == .major {
                routeAccents.color.opacity(0.25).frame(height: 2)
                Text(formattedAlert.alertCardMajorBody)
                    .font(Typography.callout)
                    .fixedSize(horizontal: false, vertical: true)

                if let onViewDetails {
                    Button(
                        action: onViewDetails,
                        label: {
                            Text("View details", comment: "Button that shows more informaton about an alert")
                                .frame(maxWidth: .infinity)
                        }
                    )
                    .foregroundStyle(routeAccents.textColor)
                    .font(Typography.bodySemibold)
                    .padding(10)
                    .frame(minHeight: 44)
                    .background(routeAccents.color)
                    .clipShape(.rect(cornerRadius: 8.0))
                    .preventScrollTaps()
                }
            }
        }
        .padding(internalPadding)
        .padding(16)
        .background(Color.fill3)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .padding(1)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.halo, lineWidth: 2))
        .padding(.top, 1)
    }

    var body: some View {
        if spec != .major, let onViewDetails {
            Button(
                action: onViewDetails,
                label: { card }
            )
            .foregroundStyle(Color.text)
            .preventScrollTaps()
        } else {
            card
        }
    }
}

#Preview {
    ScrollView {
        VStack {
            AlertCard(
                alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                    alert.header = "Orange Line suspended from point A to point B"
                    alert.effect = .suspension
                },
                alertSummary: nil,
                spec: .major,
                routeAccents: .init(color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), type: .heavyRail),
                onViewDetails: {}
            )
            .padding(32)

            AlertCard(
                alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                    alert.effect = .serviceChange
                },
                alertSummary: nil,
                spec: .secondary,
                routeAccents: .init(color: Color(hex: "80276C"), textColor: Color(hex: "FFFFFF"), type: .commuterRail),
                onViewDetails: {}
            )
            .padding(32)

            AlertCard(
                alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                    alert.effect = .elevatorClosure
                    alert.header = "Ruggles elevator 321 (Orange Line Platform to lobby) unavailable due to maintenance"
                },
                alertSummary: nil,
                spec: .elevator,
                routeAccents: .init(color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), type: .heavyRail),
                onViewDetails: {}
            )
            .padding(32)

            let objects = ObjectCollectionBuilder()
            let alert = objects.alert { alert in
                alert.effect = .shuttle
                alert.header = "Test header"
            }

            AlertCard(
                alert: alert,
                alertSummary: AlertSummary.Standard(
                    effect: .shuttle,
                    location: .some(AlertSummary.LocationSuccessiveStops(startStopName: "Start", endStopName: "End")),
                    timeframe: .some(AlertSummary.TimeframeTime(
                        time: .init(year: 2025, month: .april, day: 16, hour: 16, minute: 0, second: 0)
                    )),
                    recurrence: nil,
                    isUpdate: false
                ),
                spec: .major,
                routeAccents: .init(color: .pink, textColor: .orange, type: .ferry),
                onViewDetails: {}
            )
            .padding(32)

            AlertCard(
                alert: alert,
                alertSummary: AlertSummary.Standard(
                    effect: .shuttle,
                    location: .some(AlertSummary.LocationSuccessiveStops(startStopName: "Start", endStopName: "End")),
                    timeframe: .some(AlertSummary.TimeframeTime(
                        time: .init(year: 2025, month: .april, day: 16, hour: 16, minute: 0, second: 0)
                    )),
                    recurrence: nil,
                    isUpdate: true
                ),
                spec: .secondary,
                routeAccents: .init(color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), type: .heavyRail),
                onViewDetails: {}
            )
            .padding(32)

            AlertCard(
                alert: alert,
                alertSummary: AlertSummary.AllClear(
                    location: .some(AlertSummary.LocationSuccessiveStops(startStopName: "Start", endStopName: "End")),
                ),
                spec: .secondary,
                routeAccents: .init(color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), type: .heavyRail),
                onViewDetails: {}
            )
            .padding(32)

            AlertCard(
                alert: objects.alert { $0.effect = .cancellation },
                alertSummary: AlertSummary.TripSpecific(
                    tripIdentity: AlertSummary.TripSpecificTripFrom(
                        tripTime: .init(year: 2026, month: .march, day: 9, hour: 12, minute: 13, second: 0),
                        stopName: "Ruggles"
                    ), effect: .cancellation, cause: .mechanicalIssue
                ),
                spec: .major,
                routeAccents: .init(color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), type: .heavyRail),
                onViewDetails: {}
            )
            .padding(32)
        }
    }
    .background(Color.fill2)
}

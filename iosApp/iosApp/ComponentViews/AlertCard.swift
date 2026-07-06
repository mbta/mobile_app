//
//  AlertCard.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-15.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

private struct TakeoverAlertCard: View {
    @ObserveInjection var inject
    let alert: Shared.Alert
    let alertSummaryEntity: AlertSummaryEntity?
    let now: EasternTimeInstant
    let routeAccents: TripRouteAccents
    let onViewDetails: (() -> Void)?
    let internalPadding: EdgeInsets

    @ScaledMetric var iconSize = 48

    var formattedAlert: FormattedAlert {
        FormattedAlert(alert: alert, alertSummaryEntity: alertSummaryEntity)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(alignment: .center, spacing: 16) {
                    // Override alert state and icon size in the case of all clear
                    // TODO: prop drill now
                    let alertState = alert.allClear(atTime: .now()) ? .allClear : alert.alertState
                    AlertIcon(
                        alertState: alertState,
                        color: routeAccents.color,
                        overrideIcon: alert.effect == .cancellation ? routeSlashIcon(routeAccents.type) : nil
                    )
                    .scaledToFit()
                    .frame(width: iconSize, height: iconSize, alignment: .center)
                    Text(formattedAlert.alertCardHeader(spec: .takeover, type: routeAccents.type, now: now))
                        .font(Typography.title2Bold)
                        .multilineTextAlignment(.leading)
                }
            }

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
        .padding(internalPadding)
        .padding(16)
        .enableInjection()
    }
}

struct AlertCard: View {
    @ObserveInjection var inject
    let alert: Shared.Alert
    let alertSummaryEntity: AlertSummaryEntity?
    let spec: AlertCardSpec
    let now: EasternTimeInstant
    let routeAccents: TripRouteAccents
    let onViewDetails: (() -> Void)?
    let internalPadding: EdgeInsets

    @ScaledMetric var elevatorIconSize = 36

    @ScaledMetric var miniIconSize = 20
    @ScaledMetric var chevronIconSize = 16

    init(
        alert: Shared.Alert,
        alertSummaryEntity: AlertSummaryEntity?,
        spec: AlertCardSpec,
        now: EasternTimeInstant = .now(),
        routeAccents: TripRouteAccents,
        onViewDetails: (() -> Void)?,
        internalPadding: EdgeInsets = .init()
    ) {
        self.alert = alert
        self.alertSummaryEntity = alertSummaryEntity
        self.spec = spec
        self.now = now
        self.routeAccents = routeAccents
        self.onViewDetails = onViewDetails
        self.internalPadding = internalPadding
    }

    var formattedAlert: FormattedAlert {
        FormattedAlert(alert: alert, alertSummaryEntity: alertSummaryEntity)
    }

    var iconSize: Double {
        switch spec {
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
                    // TODO: prop drill now
                    let alertState = alert.allClear(atTime: .now()) ? .allClear : alert.alertState
                    let iconSize = alertState == .allClear ? elevatorIconSize : iconSize
                    AlertIcon(
                        alertState: alertState,
                        color: routeAccents.color,
                        overrideIcon: alert.effect == .cancellation ? routeSlashIcon(routeAccents.type) : nil
                    )
                    .scaledToFit()
                    .frame(width: iconSize, height: iconSize, alignment: .center)
                    Text(formattedAlert.alertCardHeader(spec: spec, type: routeAccents.type, now: now))
                        .font(Typography.callout)
                        .multilineTextAlignment(.leading)
                }
                Spacer()
                Image(.faChevronRight)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(height: chevronIconSize)
                    .accessibilityHidden(true)
            }
        }
        .padding(internalPadding)
        .padding(16)
    }

    var body: some View {
        if spec == .takeover {
            TakeoverAlertCard(alert: alert,
                              alertSummaryEntity: alertSummaryEntity,
                              now: now,
                              routeAccents: routeAccents,
                              onViewDetails: onViewDetails,
                              internalPadding: internalPadding)
        } else {
            if let onViewDetails {
                Button(
                    action: onViewDetails,
                    label: { card }
                )
                .foregroundStyle(Color.text)
                .preventScrollTaps()
            } else { card }
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
                alertSummaryEntity: nil,
                spec: .takeover,
                routeAccents: .init(color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), type: .heavyRail),
                onViewDetails: {}
            )
            .padding(32)

            AlertCard(
                alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                    alert.effect = .serviceChange
                },
                alertSummaryEntity: nil,
                spec: .basic,
                routeAccents: .init(color: Color(hex: "80276C"), textColor: Color(hex: "FFFFFF"), type: .commuterRail),
                onViewDetails: {}
            )
            .padding(32)

            AlertCard(
                alert: ObjectCollectionBuilder.Single.shared.alert { alert in
                    alert.effect = .elevatorClosure
                    alert.header = "Ruggles elevator 321 (Orange Line Platform to lobby) unavailable due to maintenance"
                },
                alertSummaryEntity: nil,
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
                alertSummaryEntity: .init(
                    routeId: nil,
                    stopId: nil,
                    tripId: nil,
                    directionId: nil,
                    summary: "Shuttle buses replace service from Start to End through 4:00 PM"
                ),
                spec: .takeover,
                routeAccents: .init(color: .pink, textColor: .orange, type: .ferry),
                onViewDetails: {}
            )
            .padding(32)

            AlertCard(
                alert: alert,
                alertSummaryEntity: .init(
                    routeId: nil,
                    stopId: nil,
                    tripId: nil,
                    directionId: nil,
                    summary: "Update: Shuttle buses replace service from Start to End through 4:00 PM"
                ),
                spec: .basic,
                routeAccents: .init(color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), type: .heavyRail),
                onViewDetails: {}
            )
            .padding(32)

            AlertCard(
                alert: alert,
                alertSummaryEntity: .init(
                    routeId: nil,
                    stopId: nil,
                    tripId: nil,
                    directionId: nil,
                    summary: "All clear: Regular service from Start to End"
                ),
                spec: .basic,
                routeAccents: .init(color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), type: .heavyRail),
                onViewDetails: {}
            )
            .padding(32)

            AlertCard(
                alert: objects.alert {
                    $0.cause = .mechanicalIssue
                    $0.effect = .cancellation
                },
                alertSummaryEntity: .init(
                    routeId: nil,
                    stopId: nil,
                    tripId: nil,
                    directionId: nil,
                    summary: "1:00 PM trip from Ruggles cancelled today due to mechanical issue"
                ),
                spec: .takeover,
                routeAccents: .init(color: Color(hex: "ED8B00"), textColor: Color(hex: "FFFFFF"), type: .heavyRail),
                onViewDetails: {}
            )
            .padding(32)
        }
    }
    .background(Color.fill2)
}

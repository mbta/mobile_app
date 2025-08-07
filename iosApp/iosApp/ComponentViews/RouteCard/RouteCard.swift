//
//  RouteCard.swift
//  iosApp
//
//  Created by esimon on 4/11/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteCardContainer<Content: View>: View {
    let cardData: RouteCardData
    let onPin: (String) -> Void
    let pinned: Bool
    let showStopHeader: Bool
    let departureContent: (RouteCardData.RouteStopData) -> Content

    @EnvironmentObject var settingsCache: SettingsCache
    var enhancedFavorites: Bool { settingsCache.get(.enhancedFavorites) }

    var body: some View {
        VStack(spacing: 0) {
            TransitHeader(
                name: cardData.lineOrRoute.name,
                routeType: cardData.lineOrRoute.type,
                backgroundColor: Color(hex: cardData.lineOrRoute.backgroundColor),
                textColor: Color(hex: cardData.lineOrRoute.textColor),
                rightContent: {
                    HStack {
                        if !enhancedFavorites {
                            StarButton(
                                starred: pinned,
                                color: Color(hex: cardData.lineOrRoute.textColor),
                                action: { onPin(cardData.lineOrRoute.id) }
                            )
                        }
                    }
                }
            )
            .accessibilityElement(children: .contain)
            ForEach(Array(cardData.stopData.enumerated()), id: \.element) { index, stopData in
                if showStopHeader {
                    RouteCardStopHeader(
                        data: stopData
                    )
                }
                departureContent(stopData)

                if index < cardData.stopData.count - 1 {
                    HaloSeparator()
                }
            }
        }
        .background(Color.fill3)
        .withRoundedBorder()
    }
}

struct RouteCard: View {
    let cardData: RouteCardData
    let global: GlobalResponse?
    let now: EasternTimeInstant
    let onPin: (String) -> Void
    let pinned: Bool
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let showStopHeader: Bool

    @EnvironmentObject var settingsCache: SettingsCache
    var enhancedFavorites: Bool { settingsCache.get(.enhancedFavorites) }
    var showStationAccessibility: Bool { settingsCache.get(.stationAccessibility) }

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    var body: some View {
        RouteCardContainer(cardData: cardData,
                           onPin: onPin,
                           pinned: pinned,
                           showStopHeader: showStopHeader) { stopData in
            RouteCardDepartures(
                stopData: stopData,
                global: global,
                now: now,
                pinned: pinned,
                pushNavEntry: pushNavEntry
            )
        }
    }
}

private func cardForPreview(_ card: RouteCardData, _ previewData: RouteCardPreviewData) -> some View {
    RouteCard(
        cardData: card,
        global: previewData.global,
        now: previewData.now,
        onPin: { _ in },
        pinned: false,
        pushNavEntry: { _ in },
        showStopHeader: true
    )
}

#Preview {
    let data = RouteCardPreviewData()
    ScrollView {
        VStack {
            Section("Orange Line disruption") {
                // Downstream disruption
                cardForPreview(data.OL1(), data)
                // Disrupted stop
                cardForPreview(data.OL2(), data)
            }
            Section("Red Line branching") {
                // Show up to the next three trips in the branching direction
                cardForPreview(data.RL1(), data)
                // Next three trips go to the same destination
                cardForPreview(data.RL2(), data)
                // Predictions unavailable for a branch
                cardForPreview(data.RL3(), data)
                // Service not running on a branch downstream
                cardForPreview(data.RL4(), data)
                // Service disrupted on a branch downstream
                cardForPreview(data.RL5(), data)
            }
            Section("Green Line Branching") {
                // Branching in both directions
                cardForPreview(data.GL1(), data)
                // Downstream disruption
                cardForPreview(data.GL2(), data)
            }
            Section("Silver Line Branching") {
                // Branching in one direction
                cardForPreview(data.SL1(), data)
            }
            Section("CR Branching") {
                // Branching in one direction
                cardForPreview(data.CR1(), data)
            }
            Section("Bus Route Single Direction") {
                // "Next two trips go to the same destination"
                cardForPreview(data.Bus1(), data)
                // "Next two trips go to different destinations"
                cardForPreview(data.Bus2(), data)
                // "Next two trips go to different destinations"
                cardForPreview(data.Bus3(), data)
            }
            Section("Service ended") {
                // Service ended on a branch
                cardForPreview(data.RL6(), data)
                // Service ended on all branches
                cardForPreview(data.RL7(), data)
                // Predictions unavailable on a branch
                cardForPreview(data.GL3(), data)
                // Predictions unavailable on all branches
                cardForPreview(data.GL4(), data)
            }
            Section("Disruption") {
                // Disruption on a branch
                cardForPreview(data.GL5(), data)
                // "Disruption on a branch, predictions unavailable for other branches"
                cardForPreview(data.GL6(), data)
                // "Disruption on all branches"
                cardForPreview(data.GL7(), data)
            }
        }
        .padding()
    }
    .withFixedSettings([:])
}

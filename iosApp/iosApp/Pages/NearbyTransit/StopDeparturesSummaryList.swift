//
//  StopDeparturesSummaryList.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct StopDeparturesSummaryList: View {
    let patternsByStop: PatternsByStop
    let condenseHeadsignPredictions: Bool
    let now: Instant
    let pushNavEntry: (SheetNavigationStackEntry) -> Void

    var body: some View {
        ForEach(
            Array(patternsByStop.patterns.enumerated()),
            id: \.element.id
        ) { index, patterns in
            VStack(spacing: 0) {
                SheetNavigationLink(
                    value: .stopDetails(
                        patternsByStop.stop,
                        filterFor(patterns: patterns)
                    ),
                    action: pushNavEntry
                ) {
                    DestinationView(
                        patterns: patterns,
                        now: now,
                        singleHeadsignPredictions: condenseHeadsignPredictions
                    )
                }
                .padding(8)
                .frame(minHeight: 44)
                .padding(.leading, 8)

                if index < patternsByStop.patterns.count - 1 {
                    Divider().background(Color.halo)
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityHint(Text("Open for more arrivals"))
    }

    func filterFor(patterns: RealtimePatterns) -> StopDetailsFilter {
        switch patterns as AnyObject {
        case let patternsByHeadsign as RealtimePatterns.ByHeadsign:
            .init(
                routeId: patternsByStop.routeIdentifier,
                directionId: patternsByHeadsign.directionId()
            )
        case let patternsByDirection as RealtimePatterns.ByDirection:
            .init(
                routeId: patternsByStop.routeIdentifier,
                directionId: patternsByDirection.directionId()
            )
        default:
            .init(routeId: "", directionId: 0)
        }
    }
}

struct DestinationView: View {
    let patterns: RealtimePatterns
    let condenseHeadsignPredictions: Bool
    let now: Instant

    init(patterns: RealtimePatterns, now: Instant, singleHeadsignPredictions: Bool = false) {
        self.patterns = patterns
        self.now = now
        condenseHeadsignPredictions = singleHeadsignPredictions
    }

    var body: some View {
        switch patterns as AnyObject {
        case let patternsByHeadsign as RealtimePatterns.ByHeadsign:
            HeadsignRowView(
                headsign: patternsByHeadsign.headsign,
                predictions: patternsByHeadsign.format(now: now, count: condenseHeadsignPredictions ? 1 : 2),
                routeType: patternsByHeadsign.route.type,
                pillDecoration: patternsByHeadsign.line != nil ?
                    .onRow(route: patternsByHeadsign.route) : .none
            )
        case let patternsByDirection as RealtimePatterns.ByDirection:
            HeadsignRowView(
                direction: patternsByDirection.direction,
                predictions: patternsByDirection.format(now: now),
                routeType: patternsByDirection.representativeRoute.type,
                pillDecoration: .onPrediction(routesByTrip: patternsByDirection.routesByTrip)
            )
        default:
            EmptyView()
        }
    }
}

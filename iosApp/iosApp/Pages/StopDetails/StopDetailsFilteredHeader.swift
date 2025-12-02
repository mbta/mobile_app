//
//  StopDetailsFilteredHeader.swift
//  iosApp
//
//  Created by esimon on 11/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopDetailsFilteredHeader: View {
    var route: Route?
    var line: Line?
    var stop: Stop?
    var direction: Int32
    var isFavorite: Bool = false
    var onFavorite: () -> Void = {}
    var navCallbacks: NavigationCallbacks

    var body: some View {
        let routeLabel = line?.longName ?? route?.label
        let routeTypeLabel = if let type = route?.type, type != .ferry {
            type.typeText(isOnly: true)
        } else { "" }
        let accessibilityLabel = if let routeLabel, let stop {
            Text(
                "\(routeLabel) \(routeTypeLabel) at \(stop.name)",
                comment: """
                VoiceOver text for the stop details page header,
                describes the selected route and and stop, ex '[Red Line] [train] at [Porter]'
                """
            )
        } else {
            Text(
                "Stop details",
                comment: "Stop details header fallback voiceover text when data fails to load"
            )
        }
        SheetHeader(
            title: {
                HStack(alignment: .center, spacing: 8) {
                    let pillAccessibilityLabel =
                        routeModeLabel(line: line, route: route)
                    if let line {
                        RoutePill(route: nil, line: line, type: .fixed)
                            .accessibilityLabel(pillAccessibilityLabel)
                    } else if let route {
                        RoutePill(route: route, type: .fixed)
                            .accessibilityLabel(pillAccessibilityLabel)
                    }
                    if let stop {
                        Text(stopLabel(stop)).font(Typography.headline).layoutPriority(1)
                    }
                    Spacer(minLength: 0)
                }
                .accessibilityElement()
                .accessibilityAddTraits(.isHeader)
                .accessibilityHeading(.h1)
                .accessibilityLabel(accessibilityLabel)
            },
            buttonColor: .text.opacity(0.6),
            buttonTextColor: .fill2,
            navCallbacks: navCallbacks,
            rightActionContents: {
                StarButton(starred: isFavorite, color: Color.text, action: onFavorite)
                    .id(direction) // don’t play animation when switching between favorited direction and unfavorited
                    // direction
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: 44, maxHeight: 44)
            }
        )
        .padding(.bottom, 16)
    }

    func stopLabel(_ stop: Stop) -> AttributedString {
        let text = String(format: NSLocalizedString(
            "at **%1$@**",
            comment: """
            Label for the stop a selected route is displayed at.
            For example "[Red Line] at [Porter]".
            The station name between the asterisks is bolded
            """
        ), stop.name)
        return AttributedString.tryMarkdown(text)
    }
}

struct StopDetailsFilteredHeader_Previews: PreviewProvider {
    static var previews: some View {
        let objects = ObjectCollectionBuilder()
        let route = objects.route { route in
            route.type = .heavyRail
            route.color = "DA291C"
            route.longName = "Red Line"
            route.textColor = "FFFFFF"
        }
        let stop = objects.stop { stop in
            stop.name = "Alewife"
        }
        let longestStopNameRoute = objects.route { route in
            route.type = .bus
            route.color = "FFC72C"
            route.shortName = "441"
            route.textColor = "000000"
        }
        let longestStopName = objects.stop { stop in
            stop.name = "Hynes Convention Center - Commonwealth Ave @ Massachusetts Ave"
        }
        let crRoute = objects.route { route in
            route.type = .commuterRail
            route.color = "80276C"
            route.longName = "Providence/Stoughton Line"
            route.textColor = "FFFFFF"
        }
        let crStop = objects.stop { stop in
            stop.name = "South Station"
        }
        VStack {
            StopDetailsFilteredHeader(
                route: route,
                line: nil,
                stop: stop,
                direction: 0,
                navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating)
            )
            StopDetailsFilteredHeader(
                route: longestStopNameRoute,
                line: nil,
                stop: longestStopName,
                direction: 0,
                navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating)
            )
            StopDetailsFilteredHeader(
                route: crRoute,
                line: nil,
                stop: crStop,
                direction: 0,
                navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating)
            )
        }
    }
}

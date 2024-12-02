//
//  StopDetailsFilteredHeader.swift
//  iosApp
//
//  Created by esimon on 11/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct StopDetailsFilteredHeader: View {
    var route: Route?
    var line: Line?
    var stop: Stop?
    var pinned: Bool = false
    var onPin: () -> Void = {}
    var onClose: () -> Void = {}

    var body: some View {
        let accessibilityLabel = if let route, let stop {
            Text(
                "\(route.label) \(route.type.typeText(isOnly: true)) at \(stop.name)",
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
        HStack(alignment: .center, spacing: 16) {
            HStack(alignment: .center, spacing: 8) {
                if let line {
                    RoutePill(route: nil, line: line, type: .fixed)
                } else if let route {
                    RoutePill(route: route, type: .fixed)
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

            HStack(alignment: .center, spacing: 16) {
                PinButton(pinned: pinned, action: onPin)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: 44, maxHeight: 44)
                ActionButton(kind: .close, action: onClose)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 11)
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
        do {
            return try AttributedString(markdown: text)
        } catch {
            return AttributedString(text.filter { $0 != "*" })
        }
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
            StopDetailsFilteredHeader(route: route, line: nil, stop: stop)
            StopDetailsFilteredHeader(route: longestStopNameRoute, line: nil, stop: longestStopName)
            StopDetailsFilteredHeader(route: crRoute, line: nil, stop: crStop)
        }
    }
}

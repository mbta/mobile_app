//
//  RoutePill.swift
//  iosApp
//
//  Created by Simon, Emma on 2/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct RoutePill: View {
    enum `Type` {
        case fixed
        case flex
    }

    let route: Route?
    let type: `Type`
    let isActive: Bool
    let textColor: Color?
    let routeColor: Color?

    static let inactiveTextColor: Color = .white
    static let inactiveColor: Color = .gray.opacity(0.5)

    init(route: Route?, type: Type, isActive: Bool = true) {
        self.route = route
        self.type = type
        self.isActive = isActive
        textColor = route?.textColor != nil ? Color(hex: route!.textColor) : nil
        routeColor = route?.color != nil ? Color(hex: route!.color) : nil
    }

    enum PillContent {
        case empty
        case text(String)
        case image(ImageResource)
    }

    func getPillContent() -> PillContent {
        guard let route else { return .empty }
        if route.type == .heavyRail, type == .fixed {
            return .text(String(route.longName.split(separator: " ").compactMap(\.first)))
        }
        if route.type == .lightRail {
            if route.longName.starts(with: "Green Line ") {
                if type == .fixed {
                    return .text(route.longName.replacing("Green Line ", with: "GL "))
                } else {
                    return .text(route.shortName)
                }
            } else if route.longName == "Mattapan Trolley", type == .fixed {
                return .text("M")
            }
        }
        if route.type == .commuterRail {
            if type == .fixed {
                return .text("CR")
            }
        }
        if route.type == .ferry {
            if type == .fixed {
                return .image(.modeFerry)
            }
        }
        return switch route.type {
        case .bus:
            .text(route.shortName)
        default:
            .text(route.longName)
        }
    }

    @ViewBuilder func getPillBase() -> some View {
        switch getPillContent() {
        case .empty: EmptyView()
        case let .text(text): Text(text)
        case let .image(image): Image(image)
        }
    }

    private struct PillModifier: ViewModifier {
        let pill: RoutePill

        func body(content: Content) -> some View {
            if pill.type == .fixed {
                content.frame(width: 50, height: 24)
            } else if pill.route?.longName.starts(with: "Green Line ") ?? false {
                content.frame(width: 24, height: 24)
            } else {
                content.frame(height: 24).padding(.horizontal, 12)
            }
        }
    }

    var body: some View {
        if route == nil {
            EmptyView()
        } else {
            getPillBase()
                .textCase(.uppercase)
                .font(.custom("Helvetica Neue", size: 16).bold())
                .tracking(0.5)
                .modifier(PillModifier(pill: self))
                .lineLimit(1)
                .foregroundColor(isActive ? textColor : Self.inactiveTextColor)
                .background(isActive ? routeColor : Self.inactiveColor)
                .clipShape(Capsule())
        }
    }
}

struct RoutePill_Previews: PreviewProvider {
    struct RoutePillPreview: View {
        let route: Route

        var body: some View {
            GridRow {
                RoutePill(route: route, type: .fixed)
                RoutePill(route: route, type: .flex)
            }
        }
    }

    static var previews: some View {
        Grid(alignment: .leading, horizontalSpacing: 24, verticalSpacing: 24) {
            GridRow {
                Text(verbatim: "Fixed")
                Text(verbatim: "Flex")
            }
            RoutePillPreview(route: .init(
                id: "Red",
                type: .heavyRail,
                color: "DA291C",
                directionNames: ["South", "North"],
                directionDestinations: ["Ashmont/Braintree", "Alewife"],
                longName: "Red Line",
                shortName: "",
                sortOrder: 10010,
                textColor: "FFFFFF",
                routePatternIds: nil
            ))
            RoutePillPreview(route: .init(
                id: "Orange",
                type: .heavyRail,
                color: "ED8B00",
                directionNames: ["South", "North"],
                directionDestinations: ["Forest Hills", "Oak Grove"],
                longName: "Orange Line",
                shortName: "",
                sortOrder: 10020,
                textColor: "FFFFFF",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "Blue",
                type: .heavyRail,
                color: "003DA5",
                directionNames: ["West", "East"],
                directionDestinations: ["Bowdoin", "Wonderland"],
                longName: "Blue Line",
                shortName: "",
                sortOrder: 10040,
                textColor: "FFFFFF",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "Mattapan",
                type: .lightRail,
                color: "DA291C",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Mattapan", "Ashmont"],
                longName: "Mattapan Trolley",
                shortName: "",
                sortOrder: 10011,
                textColor: "FFFFFF",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "Green-B",
                type: .lightRail,
                color: "00843D",
                directionNames: ["West", "East"],
                directionDestinations: ["Boston College", "Government Center"],
                longName: "Green Line B",
                shortName: "B",
                sortOrder: 10032,
                textColor: "FFFFFF",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "Green-C",
                type: .lightRail,
                color: "00843D",
                directionNames: ["West", "East"],
                directionDestinations: ["Cleveland Circle", "Government Center"],
                longName: "Green Line C",
                shortName: "C",
                sortOrder: 10033,
                textColor: "FFFFFF",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "Green-D",
                type: .lightRail,
                color: "00843D",
                directionNames: ["West", "East"],
                directionDestinations: ["Riverside", "Union Square"],
                longName: "Green Line D",
                shortName: "D",
                sortOrder: 10034,
                textColor: "FFFFFF",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "Green-E",
                type: .lightRail,
                color: "00843D",
                directionNames: ["West", "East"],
                directionDestinations: ["Heath Street", "Medford/Tufts"],
                longName: "Green Line E",
                shortName: "E",
                sortOrder: 10035,
                textColor: "FFFFFF",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "CR-Fitchburg",
                type: .commuterRail,
                color: "80276C",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Wachusett", "North Station"],
                longName: "Fitchburg Line",
                shortName: "",
                sortOrder: 20012,
                textColor: "FFFFFF",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "216",
                type: .bus,
                color: "FFC72C",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Houghs Neck", "Quincy Center Station"],
                longName: "Houghs Neck - Quincy Center Station via Germantown",
                shortName: "216",
                sortOrder: 52160,
                textColor: "000000",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "627",
                type: .bus,
                color: "FFC72C",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Bedford VA Hospital", "Alewife Station"],
                longName: "Bedford VA Hospital - Alewife Station via Hanscom Airport",
                shortName: "62/76",
                sortOrder: 50621,
                textColor: "000000",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "741",
                type: .bus,
                color: "7C878E",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Logan Airport Terminals", "South Station"],
                longName: "Logan Airport Terminals - South Station",
                shortName: "SL1",
                sortOrder: 10051,
                textColor: "FFFFFF",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "Boat-F1",
                type: .ferry,
                color: "008EAA",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Hingham or Hull", "Long Wharf or Rowes Wharf"],
                longName: "Hingham/Hull Ferry",
                shortName: "",
                sortOrder: 30002,
                textColor: "FFFFFF",
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: "Shuttle-BroadwayKendall",
                type: .bus,
                color: "FFC72C",
                directionNames: ["South", "North"],
                directionDestinations: ["Ashmont/Braintree", "Alewife"],
                longName: "Kendall/MIT - Broadway via Downtown Crossing",
                shortName: "Red Line Shuttle",
                sortOrder: 61050,
                textColor: "000000",
                routePatternIds: nil
            ))
        }
    }
}

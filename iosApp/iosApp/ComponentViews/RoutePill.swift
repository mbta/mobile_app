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
    let line: Line?
    let type: `Type`
    let isActive: Bool
    let textColor: Color?
    let routeColor: Color?

    init(route: Route?, line: Line? = nil, type: Type, isActive: Bool = true) {
        self.route = route
        self.line = line
        self.type = type
        self.isActive = isActive
        guard let route else {
            guard let line else {
                textColor = nil
                routeColor = nil
                return
            }
            textColor = Color(hex: line.textColor)
            routeColor = Color(hex: line.color)
            return
        }
        if route.id.starts(with: "Shuttle"), let line {
            textColor = Color(hex: line.textColor)
            routeColor = Color(hex: line.color)
        } else {
            textColor = Color(hex: route.textColor)
            routeColor = Color(hex: route.color)
        }
    }

    private enum PillContent {
        case empty
        case text(String)
        case image(ImageResource)
    }

    private func getPillContent() -> PillContent {
        guard let route else {
            guard let line else {
                return .empty
            }
            return Self.linePillContent(line: line, type: type)
        }
        return switch route.type {
        case .lightRail: Self.lightRailPillContent(route: route, type: type)
        case .heavyRail: Self.heavyRailPillContent(route: route, type: type)
        case .commuterRail: Self.commuterRailPillContent(route: route, type: type)
        case .bus: Self.busPillContent(route: route, type: type)
        case .ferry: Self.ferryPillContent(route: route, type: type)
        }
    }

    private static func linePillContent(line: Line, type: Type) -> PillContent {
        if line.longName == "Green Line", type == .fixed {
            .text("GL")
        } else {
            .text(line.longName)
        }
    }

    private static func lightRailPillContent(route: Route, type: Type) -> PillContent {
        if route.longName.starts(with: "Green Line ") {
            if type == .fixed {
                .text(route.longName.replacing("Green Line ", with: "GL "))
            } else {
                .text(route.shortName)
            }
        } else if route.longName == "Mattapan Trolley", type == .fixed {
            .text("M")
        } else {
            .text(route.longName)
        }
    }

    private static func heavyRailPillContent(route: Route, type: Type) -> PillContent {
        if type == .fixed {
            .text(String(route.longName.split(separator: " ").compactMap(\.first)))
        } else {
            .text(route.longName)
        }
    }

    private static func commuterRailPillContent(route: Route, type: Type) -> PillContent {
        if type == .fixed {
            .text("CR")
        } else {
            .text(route.longName.replacing(" Line", with: ""))
        }
    }

    private static func busPillContent(route: Route, type: Type) -> PillContent {
        if route.id.starts(with: "Shuttle"), type == .fixed {
            .image(.modeBus)
        } else {
            .text(route.shortName)
        }
    }

    private static func ferryPillContent(route: Route, type: Type) -> PillContent {
        if type == .fixed {
            .image(.modeFerry)
        } else {
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

    private static func isRectangle(route: Route) -> Bool {
        route.type == .bus && !route.id.starts(with: "Shuttle")
    }

    private struct FramePaddingModifier: ViewModifier {
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

    private struct ColorModifier: ViewModifier {
        let pill: RoutePill

        func body(content: Content) -> some View {
            if pill.isActive {
                content
                    .foregroundColor(pill.textColor)
                    .background(pill.routeColor)
            } else if let route = pill.route, RoutePill.isRectangle(route: route) {
                content.overlay(
                    Rectangle().stroke(pill.routeColor ?? .deemphasized, lineWidth: 1).padding(1)
                )
            } else {
                content.overlay(
                    Capsule().stroke(pill.routeColor ?? .deemphasized, lineWidth: 1).padding(1)
                )
            }
        }
    }

    private struct ClipShapeModifier: ViewModifier {
        let pill: RoutePill

        func body(content: Content) -> some View {
            if let route = pill.route, RoutePill.isRectangle(route: route) {
                content.clipShape(Rectangle())
            } else {
                content.clipShape(Capsule())
            }
        }
    }

    var body: some View {
        if route == nil, line == nil {
            EmptyView()
        } else {
            getPillBase()
                .textCase(.uppercase)
                .font(.custom("Helvetica Neue", size: 16).bold())
                .tracking(0.5)
                .modifier(FramePaddingModifier(pill: self))
                .lineLimit(1)
                .modifier(ColorModifier(pill: self))
                .modifier(ClipShapeModifier(pill: self))
        }
    }
}

struct RoutePill_Previews: PreviewProvider {
    struct RoutePillPreview: View {
        let route: Route
        let line: Line?

        init(route: Route, line: Line? = nil) {
            self.route = route
            self.line = line
        }

        var body: some View {
            GridRow {
                RoutePill(route: route, line: line, type: .fixed, isActive: false)
                RoutePill(route: route, line: line, type: .fixed)
                RoutePill(route: route, line: line, type: .flex)
            }
        }
    }

    static var previews: some View {
        Grid(alignment: .leading, horizontalSpacing: 24, verticalSpacing: 24) {
            GridRow {
                Text(verbatim: "")
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
                lineId: "line-Red",
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
                lineId: "line-Orange",
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
                lineId: "line-Blue",
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
                lineId: "line-Mattapan",
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
                lineId: "line-Green",
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
                lineId: "line-Green",
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
                lineId: "line-Green",
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
                lineId: "line-Green",
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
                lineId: "line-Fitchburg",
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
                lineId: "line-214216",
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
                lineId: "line-6276",
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
                lineId: "line-SLWaterfront",
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
                lineId: "line-Boat-F1",
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
                lineId: "line-Red",
                routePatternIds: nil
            ), line: Line(
                id: "line-Red",
                color: "DA291C",
                longName: "Red Line",
                shortName: "",
                sortOrder: 10010,
                textColor: "FFFFFF"
            ))
        }
    }
}

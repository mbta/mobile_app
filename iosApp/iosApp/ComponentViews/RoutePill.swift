//
//  RoutePill.swift
//  iosApp
//
//  Created by Simon, Emma on 2/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RoutePill: View {
    let route: Route?
    let line: Line?
    let isActive: Bool
    let textColor: Color
    let routeColor: Color
    let spec: RoutePillSpec

    private var fontSize: CGFloat {
        switch spec.size {
        case .circleSmall, .flexPillSmall: 12
        default: 16
        }
    }

    private var iconSize: CGFloat {
        switch spec.size {
        case .circleSmall, .flexPillSmall: 16
        default: 24
        }
    }

    init(route: Route?, line: Line? = nil, type: RoutePillSpec.Type_, isActive: Bool = true) {
        self.route = route
        self.line = line
        self.isActive = isActive
        spec = .init(route: route, line: line, type: type)
        textColor = .init(hex: spec.textColor)
        routeColor = .init(hex: spec.routeColor)
    }

    init(spec: RoutePillSpec, isActive: Bool = true) {
        route = nil
        line = nil
        self.isActive = isActive
        self.spec = spec
        textColor = .init(hex: spec.textColor)
        routeColor = .init(hex: spec.routeColor)
    }

    @ViewBuilder func getPillBase() -> some View {
        switch onEnum(of: spec.content) {
        case .empty: EmptyView()
        case let .text(text): Text(text.text)
        case let .modeImage(mode):
            routeIcon(mode.mode)
                .resizable()
                .frame(width: iconSize, height: iconSize)
        }
    }

    private struct FramePaddingModifier: ViewModifier {
        let spec: RoutePillSpec

        func body(content: Content) -> some View {
            switch spec.size {
            case .fixedPill: content.frame(width: 50, height: 24)
            case .circle: content.frame(width: 24, height: 24)
            case .circleSmall: content.frame(width: 16, height: 16)
            case .flexPill: content.frame(height: 24).padding(.horizontal, 12).frame(minWidth: 44)
            case .flexPillSmall: content.frame(height: 16).padding(.horizontal, 8).frame(minWidth: 36)
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
            } else if pill.spec.shape == .rectangle {
                content.overlay(
                    RoundedRectangle(cornerRadius: 4).stroke(pill.routeColor, lineWidth: 1).padding(1)
                )
            } else {
                content.overlay(
                    Capsule().stroke(pill.routeColor, lineWidth: 1).padding(1)
                )
            }
        }
    }

    private struct ClipShapeModifier: ViewModifier {
        let spec: RoutePillSpec

        func body(content: Content) -> some View {
            switch spec.shape {
            case .rectangle: content.clipShape(RoundedRectangle(cornerRadius: 4))
            case .capsule: content.clipShape(Capsule())
            }
        }
    }

    var body: some View {
        getPillBase()
            .textCase(route?.type == .commuterRail ? .none : .uppercase)
            .font(.custom("Helvetica Neue", size: fontSize).bold())
            .tracking(0.5)
            .modifier(FramePaddingModifier(spec: spec))
            .minimumScaleFactor(0.4)
            .lineLimit(1)
            .modifier(ColorModifier(pill: self))
            .modifier(ClipShapeModifier(spec: spec))
            .accessibilityElement()
            .accessibilityLabel(routeModeLabel(line: line, route: route))
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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
                isListedRoute: true,
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

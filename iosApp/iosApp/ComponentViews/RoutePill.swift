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
    let borderWidth: CGFloat
    let borderColor: Color

    private var fontSize: CGFloat {
        switch spec.height {
        case .small: 12
        case .medium: 16
        case .large: spec.width == .circle ? 21 : 16
        }
    }

    private var pillHeight: CGFloat {
        switch spec.height {
        case .small: 16
        case .medium: 24
        case .large: spec.width == .circle ? 32 : 24
        }
    }

    init(
        route: Route?,
        line: Line? = nil,
        type: RoutePillSpec.Type_,
        height: RoutePillSpec.Height = .medium,
        isActive: Bool = true,
        borderWidth: CGFloat = 0,
        borderColor: Color = .halo,
    ) {
        self.route = route
        self.line = line
        self.isActive = isActive
        spec = .init(route: route, line: line, type: type, height: height)
        textColor = .init(hex: spec.textColor)
        routeColor = .init(hex: spec.routeColor)
        self.borderWidth = borderWidth
        self.borderColor = borderColor
    }

    init(
        spec: RoutePillSpec,
        isActive: Bool = true,
        borderWidth: CGFloat = 0,
        borderColor: Color = .halo,
    ) {
        route = nil
        line = nil
        self.isActive = isActive
        self.spec = spec
        textColor = .init(hex: spec.textColor)
        routeColor = .init(hex: spec.routeColor)
        self.borderWidth = borderWidth
        self.borderColor = borderColor
    }

    @ViewBuilder func getPillBase() -> some View {
        switch onEnum(of: spec.content) {
        case .empty: EmptyView()
        case let .text(text): Text(text.text)
        case let .modeImage(mode):
            routeIcon(mode.mode)
                .resizable()
                .frame(width: pillHeight, height: pillHeight)
        }
    }

    private struct FramePaddingModifier: ViewModifier {
        let spec: RoutePillSpec
        let pillHeight: CGFloat

        func body(content: Content) -> some View {
            switch spec.width {
            case .fixed: content.frame(width: 2 * pillHeight + 2, height: pillHeight)
            case .circle: content.frame(width: pillHeight, height: pillHeight)
            case .flex: content.frame(height: pillHeight).padding(.horizontal, pillHeight / 2)
                .frame(minWidth: pillHeight * 3 / 2 + 12)
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

    private struct BorderModifier: ViewModifier {
        let spec: RoutePillSpec
        let borderWidth: CGFloat
        let borderColor: Color

        func body(content: Content) -> some View {
            switch spec.shape {
            case .rectangle: content.background(RoundedRectangle(cornerRadius: 4).stroke(
                    borderColor,
                    lineWidth: borderWidth * 2
                ))
            case .capsule: content.background(Capsule().stroke(borderColor, lineWidth: borderWidth * 2))
            }
        }
    }

    var body: some View {
        getPillBase()
            .textCase(route?.type == .commuterRail ? .none : .uppercase)
            .font(.custom("Helvetica Neue", size: fontSize).bold())
            .tracking(spec.width != .circle ? 0.5 : 0)
            .modifier(FramePaddingModifier(spec: spec, pillHeight: pillHeight))
            .minimumScaleFactor(0.4)
            .lineLimit(1)
            .modifier(ColorModifier(pill: self))
            .modifier(ClipShapeModifier(spec: spec))
            .modifier(BorderModifier(spec: spec, borderWidth: borderWidth, borderColor: borderColor))
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
                id: .init("Red"),
                type: .heavyRail,
                color: "DA291C",
                directionNames: ["South", "North"],
                directionDestinations: ["Ashmont/Braintree", "Alewife"],
                isListedRoute: true,
                longName: "Red Line",
                shortName: "",
                sortOrder: 10010,
                textColor: "FFFFFF",
                lineId: .init("line-Red"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: .init(
                id: .init("Orange"),
                type: .heavyRail,
                color: "ED8B00",
                directionNames: ["South", "North"],
                directionDestinations: ["Forest Hills", "Oak Grove"],
                isListedRoute: true,
                longName: "Orange Line",
                shortName: "",
                sortOrder: 10020,
                textColor: "FFFFFF",
                lineId: .init("line-Orange"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("Blue"),
                type: .heavyRail,
                color: "003DA5",
                directionNames: ["West", "East"],
                directionDestinations: ["Bowdoin", "Wonderland"],
                isListedRoute: true,
                longName: "Blue Line",
                shortName: "",
                sortOrder: 10040,
                textColor: "FFFFFF",
                lineId: .init("line-Blue"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("Mattapan"),
                type: .lightRail,
                color: "DA291C",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Mattapan", "Ashmont"],
                isListedRoute: true,
                longName: "Mattapan Trolley",
                shortName: "",
                sortOrder: 10011,
                textColor: "FFFFFF",
                lineId: .init("line-Mattapan"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("Green-B"),
                type: .lightRail,
                color: "00843D",
                directionNames: ["West", "East"],
                directionDestinations: ["Boston College", "Government Center"],
                isListedRoute: true,
                longName: "Green Line B",
                shortName: "B",
                sortOrder: 10032,
                textColor: "FFFFFF",
                lineId: .init("line-Green"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("Green-C"),
                type: .lightRail,
                color: "00843D",
                directionNames: ["West", "East"],
                directionDestinations: ["Cleveland Circle", "Government Center"],
                isListedRoute: true,
                longName: "Green Line C",
                shortName: "C",
                sortOrder: 10033,
                textColor: "FFFFFF",
                lineId: .init("line-Green"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("Green-D"),
                type: .lightRail,
                color: "00843D",
                directionNames: ["West", "East"],
                directionDestinations: ["Riverside", "Union Square"],
                isListedRoute: true,
                longName: "Green Line D",
                shortName: "D",
                sortOrder: 10034,
                textColor: "FFFFFF",
                lineId: .init("line-Green"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("Green-E"),
                type: .lightRail,
                color: "00843D",
                directionNames: ["West", "East"],
                directionDestinations: ["Heath Street", "Medford/Tufts"],
                isListedRoute: true,
                longName: "Green Line E",
                shortName: "E",
                sortOrder: 10035,
                textColor: "FFFFFF",
                lineId: .init("line-Green"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("CR-Fitchburg"),
                type: .commuterRail,
                color: "80276C",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Wachusett", "North Station"],
                isListedRoute: true,
                longName: "Fitchburg Line",
                shortName: "",
                sortOrder: 20012,
                textColor: "FFFFFF",
                lineId: .init("line-Fitchburg"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("216"),
                type: .bus,
                color: "FFC72C",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Houghs Neck", "Quincy Center Station"],
                isListedRoute: true,
                longName: "Houghs Neck - Quincy Center Station via Germantown",
                shortName: "216",
                sortOrder: 52160,
                textColor: "000000",
                lineId: .init("line-214216"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("627"),
                type: .bus,
                color: "FFC72C",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Bedford VA Hospital", "Alewife Station"],
                isListedRoute: true,
                longName: "Bedford VA Hospital - Alewife Station via Hanscom Airport",
                shortName: "62/76",
                sortOrder: 50621,
                textColor: "000000",
                lineId: .init("line-6276"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("741"),
                type: .bus,
                color: "7C878E",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Logan Airport Terminals", "South Station"],
                isListedRoute: true,
                longName: "Logan Airport Terminals - South Station",
                shortName: "SL1",
                sortOrder: 10051,
                textColor: "FFFFFF",
                lineId: .init("line-SLWaterfront"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("Boat-F1"),
                type: .ferry,
                color: "008EAA",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Hingham or Hull", "Long Wharf or Rowes Wharf"],
                isListedRoute: true,
                longName: "Hingham/Hull Ferry",
                shortName: "",
                sortOrder: 30002,
                textColor: "FFFFFF",
                lineId: .init("line-Boat-F1"),
                routePatternIds: nil
            ))
            RoutePillPreview(route: Route(
                id: .init("Shuttle-BroadwayKendall"),
                type: .bus,
                color: "FFC72C",
                directionNames: ["South", "North"],
                directionDestinations: ["Ashmont/Braintree", "Alewife"],
                isListedRoute: true,
                longName: "Kendall/MIT - Broadway via Downtown Crossing",
                shortName: "Red Line Shuttle",
                sortOrder: 61050,
                textColor: "000000",
                lineId: .init("line-Red"),
                routePatternIds: nil
            ), line: Line(
                id: .init("line-Red"),
                color: "DA291C",
                longName: "Red Line",
                shortName: "",
                sortOrder: 10010,
                textColor: "FFFFFF"
            ))
        }
    }
}

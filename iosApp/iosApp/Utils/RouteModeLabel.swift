//
//  RouteModeLabel.swift
//  iosApp
//
//  Created by esimon on 8/12/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

func routeModeLabel(lineOrRoute: LineOrRoute, isOnly: Bool = true) -> String {
    routeModeLabel(name: lineOrRoute.name, type: lineOrRoute.type, isOnly: isOnly)
}

func routeModeLabel(line: Line?, route: Route?, isOnly: Bool = true) -> String {
    routeModeLabel(name: line?.longName ?? route?.label, type: route?.type, isOnly: isOnly)
}

func routeModeLabel(route: Route, isOnly: Bool = true) -> String {
    routeModeLabel(name: route.label, type: route.type, isOnly: isOnly)
}

func routeModeLabel(name: String?, type: RouteType?, isOnly: Bool = true) -> String {
    let label = Shared.routeModeLabel(name: name, type: type)
    func typeLabel(_ type: RouteType) -> String { type.typeText(isOnly: isOnly) }

    return switch onEnum(of: label) {
    case let .nameAndType(label):
        String(format: NSLocalizedString(
            "key/route_mode_label",
            comment: """
            A route label and route type pair,
            ex 'Red Line train' or '73 bus', used in connecting stop labels
            """
        ), label.name, typeLabel(label.type))
    case let .nameOnly(label): label.name
    case let .typeOnly(label): typeLabel(label.type)
    default: ""
    }
}

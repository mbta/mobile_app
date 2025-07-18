//
//  RoutePickerPathExtension.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

extension RoutePickerPath {
    var backgroundColor: Color {
        switch self {
        case is RoutePickerPath.Root: Color.fill2
        case is RoutePickerPath.Bus: Color(hex: "FFC72C")
        case is RoutePickerPath.Silver: Color(hex: "7C878E")
        case is RoutePickerPath.CommuterRail: Color(hex: "80276C")
        case is RoutePickerPath.Ferry: Color(hex: "008EAA")
        default: Color.fill2
        }
    }

    var textColor: Color {
        switch self {
        case is RoutePickerPath.Root: Color.text
        case is RoutePickerPath.Bus: Color(hex: "000000")
        default: Color(hex: "FFFFFF")
        }
    }

    var haloColor: Color {
        switch self {
        case is RoutePickerPath.Root: Color.halo
        case is RoutePickerPath.Bus: Color(hex: "1A192026")
        default: Color(hex: "26FFFFFF")
        }
    }

    @ViewBuilder
    var label: some View {
        switch self {
        case is RoutePickerPath.Bus:
            Text("Bus")
                .font(Typography.headlineBold)
                .foregroundColor(textColor)
        case is RoutePickerPath.Silver:
            HStack(spacing: 8) {
                Text("Silver Line")
                    .font(Typography.headlineBold)
                    .foregroundColor(textColor)
                Text("Bus")
                    .font(Typography.headline)
                    .foregroundColor(textColor)
            }
        case is RoutePickerPath.CommuterRail:
            Text("Commuter Rail")
                .font(Typography.headlineBold)
                .foregroundColor(textColor)
        case is RoutePickerPath.Ferry:
            Text("Ferry")
                .font(Typography.headlineBold)
                .foregroundColor(textColor)
        default: EmptyView()
        }
    }

    var routeType: RouteType {
        switch self {
        case is RoutePickerPath.Root: RouteType.heavyRail
        case is RoutePickerPath.Bus: RouteType.bus
        case is RoutePickerPath.Silver: RouteType.bus
        case is RoutePickerPath.CommuterRail: RouteType.commuterRail
        case is RoutePickerPath.Ferry: RouteType.ferry
        default: RouteType.heavyRail
        }
    }
}

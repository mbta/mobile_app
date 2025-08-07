//
//  RoutePickerRootRow.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RoutePickerRootRow: View {
    let routeType: RouteType
    let routeColor: Color
    let textColor: Color
    let onTap: () -> Void
    let label: () -> AnyView

    init(
        routeType: RouteType,
        routeColor: Color,
        textColor: Color,
        onTap: @escaping () -> Void,
        @ViewBuilder label: @escaping () -> AnyView
    ) {
        self.routeType = routeType
        self.routeColor = routeColor
        self.textColor = textColor
        self.onTap = onTap
        self.label = label
    }

    init(path: RoutePickerPath, onTap: @escaping () -> Void) {
        self.init(
            routeType: path.routeType,
            routeColor: path.backgroundColor,
            textColor: path.textColor,
            onTap: onTap,
            label: { AnyView(path.label) }
        )
    }

    init(route: RouteCardData.LineOrRoute, onTap: @escaping () -> Void) {
        let routeColor = Color(hex: route.backgroundColor)
        let textColor = Color(hex: route.textColor)
        self.init(
            routeType: route.type,
            routeColor: routeColor,
            textColor: textColor,
            onTap: onTap,
            label: {
                AnyView(
                    Text(route.name)
                        .font(Typography.headlineBold)
                        .foregroundColor(textColor)
                )
            }
        )
    }

    var body: some View {
        Button(
            action: onTap,
            label: {
                HStack(spacing: 8) {
                    routeIcon(routeType)
                        .resizable()
                        .frame(width: 24, height: 24)
                        .foregroundColor(textColor)
                    label()
                    Spacer()
                    Image(.faChevronRight)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 8)
                        .padding(.horizontal, 8)
                        .foregroundColor(textColor.opacity(0.6))
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 8)
                .padding(.vertical, 12)
                .background(routeColor)
                .withRoundedBorder(width: 2)
            }
        ).simultaneousGesture(TapGesture())
    }
}

#Preview {
    let modes = [
        RoutePickerPath.Bus(),
        RoutePickerPath.Silver(),
        RoutePickerPath.CommuterRail(),
        RoutePickerPath.Ferry(),
    ]
    VStack {
        ForEach(modes, id: \.self) { mode in
            RoutePickerRootRow(path: mode, onTap: {})
        }
    }
    .padding(.horizontal, 14)
}

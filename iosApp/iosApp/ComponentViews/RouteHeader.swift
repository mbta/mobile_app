//
//  RouteHeader.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/13/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct RouteHeader<Content: View>: View {
    let route: Route
    var rightContent: () -> Content?

    init(route: Route, @ViewBuilder rightContent: @escaping () -> Content? = { EmptyView() }) {
        self.route = route
        self.rightContent = rightContent
    }

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    var body: some View {
        TransitHeader(
            name: route.label,
            routeType: route.type,
            backgroundColor: route.uiColor,
            textColor: route.uiTextColor,
            rightContent: rightContent
        )
        .accessibilityElement(children: .contain)
    }
}

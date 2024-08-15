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
            name: routeName,
            backgroundColor: Color(hex: route.color),
            textColor: Color(hex: route.textColor),
            modeIcon: routeIcon(route),
            rightContent: rightContent
        )
    }

    private var routeName: String {
        switch route.type {
        case .bus:
            route.shortName
        case .commuterRail:
            route.longName.replacingOccurrences(of: "/", with: " / ")
        default:
            route.longName
        }
    }
}

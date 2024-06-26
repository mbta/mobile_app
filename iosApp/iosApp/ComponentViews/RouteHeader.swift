//
//  RouteHeader.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/13/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
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
        Label {
            routeName
                .accessibilityHeading(.h2)
                .multilineTextAlignment(.leading)
                .foregroundStyle(Color(hex: route.textColor))
                .textCase(.none)
                .frame(maxWidth: .infinity, maxHeight: modeIconHeight, alignment: .leading)
            rightContent()
        } icon: {
            routeIcon(route)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .scaledToFit()
                .frame(maxHeight: modeIconHeight, alignment: .topLeading)
                .foregroundStyle(Color(hex: route.textColor))
        }
        .padding(8)
        .background(Color(hex: route.color))
    }

    private var routeName: Text {
        switch route.type {
        case .bus:
            Text(route.shortName)
        case .commuterRail:
            Text(route.longName.replacingOccurrences(of: "/", with: " / ")).font(Typography.bodySemibold)
        default:
            Text(route.longName).font(Typography.bodySemibold)
        }
    }
}

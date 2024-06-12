//
//  RouteSection.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 4/16/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct RouteSection<Content: View>: View {
    let route: Route
    let pinned: Bool
    let onPin: (String) -> Void
    let content: () -> Content

    @ScaledMetric private var modeIconHeight: CGFloat = 24
    @ScaledMetric private var pinIconHeight: CGFloat = 20

    var body: some View {
        VStack(spacing: 0) {
            routeHeader
            content()
        }
        .background(Color.fill3)
        .clipShape(.rect(cornerRadius: 8.0))
        .overlay(
            // SwiftUI doesn't let you use a border and a corner radius at the same time
            RoundedRectangle(cornerRadius: 8.0)
                .stroke(Color.halo, lineWidth: 1)
        )
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }

    private var routeName: Text {
        switch route.type {
        case .bus:
            Text(route.shortName)
        case .commuterRail:
            Text(route.longName.replacingOccurrences(of: "/", with: " / ")).font(.body).bold()
        default:
            Text(route.longName).font(.body).bold()
        }
    }

    private var routeHeader: some View {
        Label {
            routeName
                .accessibilityHeading(.h2)
                .multilineTextAlignment(.leading)
                .foregroundStyle(Color(hex: route.textColor))
                .textCase(.none)
                .frame(maxWidth: .infinity, maxHeight: modeIconHeight, alignment: .leading)
            pinButton
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

    private var pinButton: some View {
        Button(
            action: {
                onPin(route.id)
            },
            label: {
                Image(pinned ? .pinnedRouteActive : .pinnedRouteInactive)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .shadow(color: Color.fill1, radius: pinned ? 1 : 0)
                    .accessibilityLabel("pin route")
            }
        )
        .accessibilityIdentifier("pinButton")
        .accessibilityAddTraits(pinned ? [.isSelected] : [])
        .frame(maxHeight: pinIconHeight)
    }
}

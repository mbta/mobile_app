//
//  StopListDisclosureGroup.swift
//  iosApp
//
//  Created by Melody Horn on 7/24/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopListDisclosureGroup: DisclosureGroupStyle {
    let routeAccents: TripRouteAccents
    let stickConnections: [(RouteBranchSegment.StickConnection, Bool)]
    let stopListContext: StopListContext

    @State var caretRotation: Angle = .zero
    @State var twistFactor: Float = 1

    func makeBody(configuration: Configuration) -> some View {
        VStack(spacing: 0) {
            Button(
                action: { withAnimation { configuration.isExpanded.toggle() } },
                label: {
                    ZStack(alignment: .bottom) {
                        HaloSeparator().padding(stopListContext == .trip ? .horizontal : .leading, 7)
                        HStack(spacing: 0) {
                            RouteLineTwist(
                                color: routeAccents.color,
                                proportionClosed: twistFactor,
                                connections: stickConnections
                            )
                            .padding(.leading, 14)
                            Image(.faCaretRight)
                                .resizable()
                                .frame(width: 6, height: 10)
                                .rotationEffect(caretRotation)
                                .foregroundStyle(Color.deemphasized)
                                .frame(width: 24, height: 24)
                            configuration.label
                        }.frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            ).onChange(of: configuration.isExpanded) { expanded in
                withAnimation(.easeInOut(duration: 0.5)) {
                    caretRotation = expanded ? .degrees(90) : .zero
                }
                withAnimation(.easeInOut(duration: 0.5)) {
                    twistFactor = expanded ? 0 : 1
                }
            }.preventScrollTaps()
            configuration.content
                .frame(height: configuration.isExpanded ? nil : 0, alignment: .top)
                .clipped()
                .accessibilityHidden(!configuration.isExpanded)
        }
    }
}

extension DisclosureGroupStyle where Self == StopListDisclosureGroup {
    static func stopList(
        routeAccents: TripRouteAccents,
        stickConnections: [(RouteBranchSegment.StickConnection, Bool)] = [(
            .init(fromStop: "", toStop: "", fromLane: .center, toLane: .center, fromVPos: .top, toVPos: .bottom),
            true
        )],
        context: StopListContext,
    ) -> StopListDisclosureGroup {
        .init(routeAccents: routeAccents, stickConnections: stickConnections, stopListContext: context)
    }
}

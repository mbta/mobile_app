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

    private var hideTwist: Bool {
        stickConnections.contains { connection, twisted in
            twisted &&
                (connection.fromVPos != RouteBranchSegment.VPos.top ||
                    connection.toVPos != RouteBranchSegment.VPos.bottom)
        }
    }

    func makeBody(configuration: Configuration) -> some View {
        VStack(spacing: 0) {
            Button(
                action: { withAnimation { configuration.isExpanded.toggle() } },
                label: {
                    ZStack(alignment: .bottom) {
                        HaloSeparator().padding(stopListContext == .trip ? .horizontal : .leading, 7)
                        HStack(spacing: 0) {
                            if hideTwist {
                                ZStack {
                                    if caretRotation != .zero {
                                        Circle()
                                            .stroke(Color.halo, lineWidth: 2)
                                            .background(Color.fill2)
                                            .frame(width: 32, height: 32)
                                    }
                                    caret
                                        .frame(width: 32, height: 32)
                                }
                                .padding(.leading, 20)
                                .padding(.trailing, 13) // There's already a leading 7pt padding on StopListRow
                            } else {
                                RouteLineTwist(
                                    color: routeAccents.color,
                                    proportionClosed: twistFactor,
                                    connections: stickConnections
                                )
                                .padding(.leading, 14)
                                caret
                                    .frame(width: 24, height: 24)
                            }
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

    private var caret: some View {
        Image(.faCaretRight)
            .resizable()
            .frame(width: 6, height: 10)
            .rotationEffect(caretRotation)
            .foregroundStyle(Color.deemphasized)
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
        .init(
            routeAccents: routeAccents,
            stickConnections: stickConnections,
            stopListContext: context,
        )
    }
}

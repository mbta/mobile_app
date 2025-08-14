//
//  CollapsableStopList.swift
//  iosApp
//
//  Created by Melody Horn on 7/8/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct CollapsableStopList<RightSideContent: View>: View {
    let lineOrRoute: RouteCardData.LineOrRoute
    let segment: RouteDetailsStopList.Segment
    let onClick: (RouteDetailsStopList.Entry) -> Void
    let isFirstSegment: Bool
    let isLastSegment: Bool
    let rightSideContent: (RouteDetailsStopList.Entry) -> RightSideContent

    @State var stopsExpanded = false

    let inspection = Inspection<Self>()

    init(
        lineOrRoute: RouteCardData.LineOrRoute,
        segment: RouteDetailsStopList.Segment,
        onClick: @escaping (RouteDetailsStopList.Entry) -> Void,
        isFirstSegment: Bool = false,
        isLastSegment: Bool = false,
        rightSideContent: @escaping (RouteDetailsStopList.Entry) -> RightSideContent
    ) {
        self.lineOrRoute = lineOrRoute
        self.segment = segment
        self.onClick = onClick
        self.isFirstSegment = isFirstSegment
        self.isLastSegment = isLastSegment
        self.rightSideContent = rightSideContent
    }

    var body: some View {
        if let stop = segment.stops.first, segment.stops.count == 1 {
            StopListRow(
                stop: stop.stop,
                stopLane: stop.stopLane,
                stickConnections: stop.stickConnections,
                onClick: { onClick(stop) },
                routeAccents: .init(route: lineOrRoute.sortRoute),
                stopListContext: .routeDetails,
                connectingRoutes: stop.connectingRoutes,
                stopPlacement: .init(isFirst: isFirstSegment, isLast: isLastSegment),
                descriptor: { Text("Less common stop").font(Typography.footnote).foregroundStyle(Color.text) },
                rightSideContent: { rightSideContent(stop) }
            ).background(Color.fill1)
                .onReceive(inspection.notice) { inspection.visit(self, $0) }
        } else {
            DisclosureGroup(isExpanded: $stopsExpanded, content: {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(segment.stops.enumerated()), id: \.offset) { index, stop in
                        StopListRow(
                            stop: stop.stop,
                            stopLane: stop.stopLane,
                            stickConnections: stop.stickConnections,
                            onClick: { onClick(stop) },
                            routeAccents: .init(route: lineOrRoute.sortRoute),
                            stopListContext: .routeDetails,
                            connectingRoutes: stop.connectingRoutes,
                            stopPlacement: .init(
                                isFirst: isFirstSegment && index == segment.stops.startIndex,
                                isLast: isLastSegment && index == segment.stops.index(before: segment.stops.endIndex)
                            ),
                            descriptor: { EmptyView() },
                            rightSideContent: { rightSideContent(stop) }
                        ).background(Color.fill1)
                            .padding(.leading, 7)
                    }
                }
            }, label: {
                VStack(alignment: .leading, spacing: 4) {
                    Text(AttributedString.tryMarkdown(String(format: NSLocalizedString(
                        "**%1$ld** less common stops",
                        comment: "Header for a list of stops that vehicles don't always stop at for a given route"
                    ), segment.stops.count)))
                        .multilineTextAlignment(.leading)
                        .foregroundStyle(Color.text)
                        .font(Typography.body)
                    Text(
                        "Only served at certain times of day",
                        comment: "Explainer text for \"Less common stops\" that vehicles don't always stop at"
                    )
                    .multilineTextAlignment(.leading)
                    .foregroundStyle(Color.deemphasized)
                    .font(Typography.footnote)
                }
                .padding(.vertical, 12)
                .padding(.trailing, 8)
            })
            .padding(.leading, -7)
            .disclosureGroupStyle(.stopList(
                routeAccents: .init(route: lineOrRoute.sortRoute),
                stickConnections: segment.twistedConnections().compactMap {
                    guard let connection = $0.first, let isTwisted = $0.second else { return nil }
                    return (connection, isTwisted.boolValue)
                },
                context: .routeDetails,
            ))
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
        }
    }
}

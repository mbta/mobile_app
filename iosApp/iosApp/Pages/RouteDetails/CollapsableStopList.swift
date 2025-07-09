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
    @EnvironmentObject var settingsCache: SettingsCache

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
                onClick: { onClick(stop) },
                routeAccents: .init(route: lineOrRoute.sortRoute),
                stopListContext: .routeDetails,
                connectingRoutes: stop.connectingRoutes,
                stopPlacement: .init(isFirst: isFirstSegment, isLast: isLastSegment, includeLineDiagram: false),
                showStationAccessibility: settingsCache.get(.stationAccessibility),
                descriptor: { Text("Less common stop").font(Typography.footnote) },
                rightSideContent: { rightSideContent(stop) }
            ).background(Color.fill1)
                .onReceive(inspection.notice) { inspection.visit(self, $0) }
        } else {
            DisclosureGroup(isExpanded: $stopsExpanded, content: {
                VStack {
                    ForEach(Array(segment.stops.enumerated()), id: \.offset) { index, stop in
                        StopListRow(
                            stop: stop.stop,
                            onClick: { onClick(stop) },
                            routeAccents: .init(route: lineOrRoute.sortRoute),
                            stopListContext: .routeDetails,
                            connectingRoutes: stop.connectingRoutes,
                            stopPlacement: .init(
                                isFirst: isFirstSegment && index == segment.stops.startIndex,
                                isLast: isLastSegment && index == segment.stops.index(before: segment.stops.endIndex),
                                includeLineDiagram: false
                            ),
                            showStationAccessibility: settingsCache.get(.stationAccessibility),
                            descriptor: { EmptyView() },
                            rightSideContent: { rightSideContent(stop) }
                        ).background(Color.fill1)
                    }
                }
            }, label: {
                VStack(spacing: 4) {
                    Text(AttributedString.tryMarkdown(String(format: NSLocalizedString(
                        "**%1$ld** less common stops",
                        comment: "Header for a list of stops that vehicles don't always stop at for a given route"
                    ), segment.stops.count)))
                        .foregroundStyle(Color.text)
                        .font(Typography.body)
                    Text(
                        "Only served at certain times of day",
                        comment: "Explainer text for \"Less common stops\" that vehicles don't always stop at"
                    )
                    .foregroundStyle(Color.deemphasized)
                    .font(Typography.footnote)
                }
            })
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
        }
    }
}

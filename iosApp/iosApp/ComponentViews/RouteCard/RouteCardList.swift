//
//  RouteCardList.swift
//  iosApp
//
//  Created by Melody Horn on 6/12/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteCardList<EmptyView: View>: View {
    let routeCardData: [RouteCardData]?
    @ViewBuilder let emptyView: () -> EmptyView
    let global: GlobalResponse?
    let now: Date
    let isFavorite: (RouteStopDirection) -> Bool
    let pushNavEntry: (SheetNavigationStackEntry) -> Void
    let showStopHeader: Bool

    var body: some View {
        if let routeCardData, !routeCardData.isEmpty {
            HaloScrollView {
                LazyVStack(alignment: .center, spacing: 18) {
                    ForEach(routeCardData, id: \.lineOrRoute.id) { routeCardData in
                        RouteCard(
                            cardData: routeCardData,
                            global: global,
                            now: now.toEasternInstant(),
                            isFavorite: isFavorite,
                            pushNavEntry: pushNavEntry,
                            showStopHeader: showStopHeader
                        )
                    }
                }
                .padding(.vertical, 4)
                .padding(.horizontal, 16)
            }
        } else if let routeCardData, routeCardData.isEmpty {
            HaloScrollView {
                VStack {
                    emptyView()
                    Spacer()
                }
                .padding(.horizontal, 16)
            }
        } else {
            ScrollView([]) {
                LazyVStack(alignment: .center, spacing: 14) {
                    ForEach(0 ..< 5) { _ in
                        LoadingRouteCard()
                    }
                }
                .padding(.vertical, 4)
                .padding(.horizontal, 16)
                .loadingPlaceholder()
            }
        }
    }
}

//
//  SearchResultsView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 10/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct SearchResultsView: View {
    private var state: SearchResultsContainer.ResultsState?
    private var handleStopTap: (String) -> Void

    init(
        state: SearchResultsContainer.ResultsState?,
        handleStopTap: @escaping (String) -> Void
    ) {
        self.state = state
        self.handleStopTap = handleStopTap
    }

    var body: some View {
        ScrollView {
            Group {
                switch state {
                case .loading:
                    LoadingResults()
                case let .recentStops(stops):
                    VStack {
                        Text("Recently Viewed")
                            .font(Typography.subheadlineSemibold)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        StopResultsView(stops: stops, handleStopTap: handleStopTap)
                    }
                    .padding(.top, 8)
                case let .results(results, includeRoutes):
                    VStack(spacing: 8) {
                        StopResultsView(stops: results.stops, handleStopTap: handleStopTap)
                        if includeRoutes, !results.routes.isEmpty {
                            RouteResultsView(routes: results.routes)
                                .padding(.top, 8)
                        }
                    }
                case .empty:
                    EmptyStateView(
                        headline: "No results found 🤔",
                        subheadline: "Try a different spelling or name."
                    )
                    .padding(.top, 16)
                case .error:
                    EmptyStateView(
                        headline: "Results failed to load ☹️",
                        subheadline: "Try your search again."
                    )
                    .padding(.top, 16)
                default:
                    EmptyView()
                }
            }
            .animation(.easeInOut(duration: 0.25), value: state)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .padding(.top, 8)
            .padding(.horizontal, 16)
        }
        .background(Color.fill1)
    }
}

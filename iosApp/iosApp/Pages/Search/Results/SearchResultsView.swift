//
//  SearchResultsView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 10/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

struct SearchResultsView: View {
    private var state: SearchViewModel.ResultsState?
    private var handleStopTap: (String) -> Void

    @EnvironmentObject var settingsCache: SettingsCache

    var includeRoutes: Bool { settingsCache.get(.searchRouteResults) }

    init(
        state: SearchViewModel.ResultsState?,
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
                case let .results(stopResults, routeResults):
                    if state?.isEmpty(includeRoutes: includeRoutes) == true {
                        EmptyStateView(
                            headline: "No results found 🤔",
                            subheadline: "Try a different spelling or name."
                        )
                        .padding(.top, 16)
                    } else {
                        VStack(spacing: 8) {
                            StopResultsView(stops: stopResults, handleStopTap: handleStopTap)
                            if includeRoutes, !routeResults.isEmpty {
                                RouteResultsView(routes: routeResults)
                                    .padding(.top, 8)
                            }
                        }
                    }
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
            .onChange(of: state) { state in
                if let state, case let .results(stopResults, _) = state, !state.isEmpty(includeRoutes: includeRoutes) {
                    let announcementString = String(format: NSLocalizedString(
                        "%ld results found",
                        comment: "Screen reader text that is announced when search results are returned"
                    ), stopResults.count)

                    if #available(iOS 17, *) {
                        var resultsFoundAnnouncement = AttributedString(announcementString)
                        resultsFoundAnnouncement.accessibilitySpeechAnnouncementPriority = .high
                        AccessibilityNotification.Announcement(resultsFoundAnnouncement).post()
                    } else {
                        UIAccessibility.post(
                            notification: .layoutChanged,
                            argument: announcementString
                        )
                    }
                }
            }
            .animation(.easeInOut(duration: 0.25), value: state)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .padding(.vertical, 16)
            .padding(.horizontal, 16)
        }
        .background(Color.fill1)
    }
}

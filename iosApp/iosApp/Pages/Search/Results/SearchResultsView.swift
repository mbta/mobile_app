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
    private var state: SearchViewModel.State
    private var handleStopTap: (String) -> Void

    @EnvironmentObject var settingsCache: SettingsCache

    var includeRoutes: Bool { settingsCache.get(.searchRouteResults) }

    init(
        state: SearchViewModel.State,
        handleStopTap: @escaping (String) -> Void
    ) {
        self.state = state
        self.handleStopTap = handleStopTap
    }

    var body: some View {
        ScrollView {
            Group {
                switch onEnum(of: state) {
                case .loading:
                    LoadingResultsView()
                case let .recentStops(state):
                    VStack {
                        Text("Recently Viewed")
                            .font(Typography.subheadlineSemibold)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        StopResultsView(stops: state.stops, handleStopTap: handleStopTap)
                    }
                case let .results(state):
                    if state.isEmpty(includeRoutes: includeRoutes) {
                        EmptyStateView(
                            headline: NSLocalizedString(
                                "No results found 🤔",
                                comment: "Displayed when search has no results"
                            ),
                            subheadline: NSLocalizedString(
                                "Try a different spelling or name.",
                                comment: "Displayed when search has no results"
                            ),
                        )
                        .padding(.top, 16)
                    } else {
                        VStack(spacing: 8) {
                            StopResultsView(stops: state.stops, handleStopTap: handleStopTap)
                            if includeRoutes, !state.routes.isEmpty {
                                RouteResultsView(routes: state.routes)
                                    .padding(.top, 8)
                            }
                        }
                    }
                case .error:
                    EmptyStateView(
                        headline: NSLocalizedString(
                            "Results failed to load ☹️",
                            comment: "Displayed when search encounters an error"
                        ),
                        subheadline: NSLocalizedString(
                            "Try your search again.",
                            comment: "Displayed when search encounters an error"
                        ),
                    )
                    .padding(.top, 16)
                }
            }
            .onChange(of: state) { state in
                if case let .results(state) = onEnum(of: state), !state.isEmpty(includeRoutes: includeRoutes) {
                    let announcementString = String(format: NSLocalizedString(
                        "%ld results found",
                        comment: "Screen reader text that is announced when search results are returned"
                    ), state.stops.count)

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

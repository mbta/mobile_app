//
//  ErrorBanner.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-09-25.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct ErrorBanner: View {
    @ObservedObject var errorBannerVM: ErrorBannerViewModel

    let inspection = Inspection<Self>()

    init(_ errorBannerVM: ErrorBannerViewModel) {
        self.errorBannerVM = errorBannerVM
    }

    var body: some View {
        content.onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    @ViewBuilder private var content: some View {
        let state = errorBannerVM.errorState
        switch onEnum(of: state) {
        case let .dataError(state):
            IconCard(
                iconName: "xmark.octagon",
                details: Text("Error loading data")
            ) {
                AnyView(Button(action: {
                    repo.clearState()
                    state.action()
                }, label: {
                    Image(systemName: "arrow.clockwise")
                        .accessibilityLabel("Reload data")
                }))
            }
        case let .stalePredictions(state):
            if errorBannerVM.loadingWhenPredictionsStale {
                ProgressView()
            } else {
                IconCard(
                    iconName: "clock.arrow.circlepath",
                    details: Text("Updated \(state.minutesAgo(), specifier: "%ld") minutes ago")
                ) {
                    AnyView(Button(action: {
                        repo.clearState()
                        state.action()
                    }, label: {
                        Image(systemName: "arrow.clockwise")
                            .accessibilityLabel("Refresh predictions")
                    }))
                }
            }
        case nil:
            // for some reason, .collect on an EmptyView doesn't work
            ZStack {}
        }
    }
}

#Preview {
    VStack(spacing: 16) {
        ErrorBanner(ErrorBannerViewModel(errorRepository: MockErrorBannerStateRepository(
            state: .DataError(action: {})
        )))
        ErrorBanner(ErrorBannerViewModel(errorRepository: MockErrorBannerStateRepository(
            state: .StalePredictions(lastUpdated: Date.now.addingTimeInterval(-2 * 60).toKotlinInstant(), action: {})
        )))
        ErrorBanner(ErrorBannerViewModel(
            errorRepository: MockErrorBannerStateRepository(
                state: .StalePredictions(
                    lastUpdated: Date.now.addingTimeInterval(-2 * 60).toKotlinInstant(),
                    action: {}
                )
            ),
            initialLoadingWhenPredictionsStale: true
        ))
    }
}

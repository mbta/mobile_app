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

    let minHeight = 60.0
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
            ErrorCard { Text("Error loading data") }
                .refreshable(label: "Reload data") {
                    errorBannerVM.clearState()
                    state.action()
                }
                .frame(minHeight: minHeight)
        case let .stalePredictions(state):
            if errorBannerVM.loadingWhenPredictionsStale {
                ProgressView().frame(minHeight: minHeight)
            } else {
                ErrorCard {
                    Text("Updated \(state.minutesAgo(), specifier: "%ld") minutes ago")
                }
                .refreshable(label: "Refresh predictions") {
                    errorBannerVM.clearState()
                    state.action()
                }
                .frame(minHeight: minHeight)
            }
        case .networkError:
            ErrorCard { HStack {
                Image(systemName: "wifi.slash")
                Text("Unable to connect")
            }
            }
        case nil:
            // for some reason, .collect on an EmptyView doesn't work
            EmptyView()
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

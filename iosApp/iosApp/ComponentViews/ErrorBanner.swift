//
//  ErrorBanner.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-09-25.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct ErrorBanner: View {
    let errorBannerVM: IErrorBannerViewModel
    @State var errorBannerVMState: ErrorBannerViewModel.State = .init()

    let minHeight = 60.0
    let inspection = Inspection<Self>()

    init(_ errorBannerVM: IErrorBannerViewModel) {
        self.errorBannerVM = errorBannerVM
    }

    var body: some View {
        VStack {
            content
        }
        .task {
            for await model in errorBannerVM.models {
                errorBannerVMState = model
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    @ViewBuilder private var content: some View {
        let state = errorBannerVMState.errorState
        switch onEnum(of: state) {
        case let .dataError(state):
            ErrorCard {
                VStack {
                    Text("Error loading data", comment: "Displayed when loading necessary information fails")

                    DebugView {
                        ForEach(state.messages.sorted(), id: \.self) { errorName in
                            Text(errorName)
                        }
                    }
                }
            }
            .refreshable(
                label: NSLocalizedString("Reload data", comment: "Refresh button label")
            ) {
                state.action()
                errorBannerVM.clearState()
            }
            .frame(minHeight: minHeight)
        case let .stalePredictions(state):
            if errorBannerVMState.loadingWhenPredictionsStale {
                ProgressView().frame(minHeight: minHeight)
            } else {
                ErrorCard {
                    Text(
                        "Updated \(state.minutesAgo(), specifier: "%ld") minutes ago",
                        comment: "Displayed when prediction data has not been able to update for an unexpected amount of time"
                    )
                }
                .refreshable(
                    label: NSLocalizedString(
                        "Refresh predictions",
                        comment: "Refresh button label for reloading predictions"
                    )
                ) {
                    state.action()
                    errorBannerVM.clearState()
                }
                .frame(minHeight: minHeight)
            }
        case .networkError:
            ErrorCard { HStack {
                Image(systemName: "wifi.slash")
                Text("Unable to connect", comment: "Displayed when the phone is not connected to the network")
                Spacer()
            }}
        case nil:
            EmptyView()
        }
    }
}

#Preview {
    VStack(spacing: 16) {
        ErrorBanner(MockErrorBannerViewModel(initialState: .init(
            loadingWhenPredictionsStale: false,
            errorState: .DataError(messages: Set(), action: {})
        )))
        ErrorBanner(MockErrorBannerViewModel(initialState: .init(loadingWhenPredictionsStale: false,
                                                                 errorState: .StalePredictions(
                                                                     lastUpdated: EasternTimeInstant.now()
                                                                         .minus(minutes: 2),
                                                                     action: {}
                                                                 ))))
        ErrorBanner(MockErrorBannerViewModel(initialState:
            .init(loadingWhenPredictionsStale: true,
                  errorState: .StalePredictions(
                      lastUpdated: EasternTimeInstant.now().minus(minutes: 2),
                      action: {}
                  ))))
    }
    .withFixedSettings([:])
}

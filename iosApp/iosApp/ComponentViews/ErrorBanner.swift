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
    var repo: IErrorBannerStateRepository
    @State var state: ErrorBannerState?

    let inspection = Inspection<Self>()

    init(repo: IErrorBannerStateRepository = RepositoryDI().errorBanner) {
        self.repo = repo
        state = repo.state.value
    }

    var body: some View {
        content
            .collect(flow: repo.state, into: $state)
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    @ViewBuilder private var content: some View {
        switch onEnum(of: state) {
        case let .stalePredictions(state):
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
        default:
            // for some reason, .collect on an EmptyView doesn't work
            ZStack {}
        }
    }
}

#Preview {
    ErrorBanner(repo: MockErrorBannerStateRepository(
        state: .StalePredictions(lastUpdated: Date.now.addingTimeInterval(-2 * 60).toKotlinInstant(), action: {})
    ))
}

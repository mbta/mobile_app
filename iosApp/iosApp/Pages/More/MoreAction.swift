//
//  MoreAction.swift
//  iosApp
//
//  Created by esimon on 3/17/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Sentry
import Shared
import SwiftUI

struct MoreAction: View {
    @ObserveInjection var inject
    var label: String
    var callback: () -> Void

    var body: some View {
        Button(action: {
            Shared.Sentry.shared.captureMessage(message: "Sentry SDK from Swift via KMP works")
            SentrySDK.addBreadcrumb(.init(level: .warning, category: "Huh"))
            SentrySDK.capture(message: "Sentry SDK directly from Swift works")
            callback()
        }) {
            HStack(alignment: .center, spacing: 0) {
                Text(label)
                    .foregroundStyle(Color.text)
                    .font(Typography.body)
            }
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 16)
        .frame(minHeight: 44)
        .enableInjection()
    }
}

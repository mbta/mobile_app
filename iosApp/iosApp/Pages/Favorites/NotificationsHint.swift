//
//  NotificationsHint.swift
//  iosApp
//
//  Created by esimon on 3/17/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct NotificationsHint: View {
    var onTap: () -> Void
    var onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .trailing, spacing: 0) {
            Image(.nub).foregroundStyle(Color.key).padding(.trailing, 20).padding(.bottom, -1)
            HStack(alignment: .center, spacing: 8) {
                Text("Now \(Text("edit your favorites").underline()) to get disruption notifications")
                    .foregroundStyle(Color.fill3)
                    .font(Typography.body)
                    .frame(maxWidth: .infinity, alignment: .leading)
                ActionButton(
                    kind: .close,
                    circleColor: .key,
                    iconColor: .fill3,
                    action: onDismiss
                ).overlay(Circle()
                    .stroke(Color.haloContrast, lineWidth: 2)
                    .frame(width: 34, height: 34))
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.key)
            .withRoundedBorder(color: .clear, opacity: 1, width: 0)
            .onTapGesture(perform: onTap)
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 14)
    }
}

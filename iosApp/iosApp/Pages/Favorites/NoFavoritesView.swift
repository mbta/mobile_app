//
//  NoFavoritesView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/14/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

struct NoFavoritesView: View {
    @ObserveInjection var inject
    var onAddStops: (() -> Void)?

    var body: some View {
        VStack(spacing: 32) {
            Text(
                "Add favorite stops for easy access and disruption notifications",
                comment: "Indicates the absence of favorites"
            )
            .multilineTextAlignment(.center)
            .font(Typography.title3)
            .foregroundColor(.deemphasized)

            if let onAddStops {
                Button(
                    action: onAddStops,
                    label: {
                        HStack(alignment: .center, spacing: 16) {
                            Text("Add favorite stops")
                                .font(Typography.bodySemibold)
                                .foregroundColor(.fill3)
                            StarIcon(starred: true, color: .fill3, size: 24).accessibilityHidden(true)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color.key)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                )
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 64)
        .padding(.bottom, 16)
        .enableInjection()
    }
}

#Preview {
    VStack(spacing: 16) {
        NoFavoritesView()
        Divider()
        NoFavoritesView(onAddStops: {})
    }
}

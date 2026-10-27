//
//  FavoriteDeleteButton.swift
//  iosApp
//
//  Created by esimon on 11/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct FavoriteDeleteButton: View {
    @ObserveInjection var inject
    var onDelete: () -> Void

    var body: some View {
        Button(
            action: onDelete,
            label: {
                HStack(alignment: .center, spacing: 16) {
                    Text("Remove from Favorites", comment: "Button to delete an individual favorite")
                        .font(Typography.bodySemibold)
                    Image(.faDelete).resizable().scaledToFit().frame(width: 16, height: 16)
                }.frame(maxWidth: .infinity, alignment: .center)
            }
        )
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .frame(minHeight: 45)
        .background(Color.delete)
        .foregroundStyle(Color.deleteBackground)
        .withRoundedBorder(width: 0)
        .enableInjection()
    }
}

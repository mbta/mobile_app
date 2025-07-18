//
//  NoFavoritesView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/14/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

struct NoFavoritesView: View {
    var onAddStops: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 32) {
            Text("No stops added", comment: "Indicates the absence of favorites")
                .font(Typography.title3)
                .foregroundColor(.deemphasized)
            Image(.noFavorites)
                .resizable()
                .frame(width: 56, height: 56)
                .foregroundColor(.deemphasized)
            if let onAddStops {
                Button(
                    action: onAddStops,
                    label: {
                        HStack(alignment: .center, spacing: 16) {
                            Text("Add Stops")
                                .font(Typography.bodySemibold)
                                .foregroundColor(.fill3)
                            Image(.plus)
                                .resizable()
                                .frame(width: 13, height: 13)
                                .foregroundColor(.fill3)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color.key)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                )
            }
        }
    }
}

#Preview {
    VStack(spacing: 16) {
        NoFavoritesView()
        Divider()
        NoFavoritesView(onAddStops: {})
    }
}

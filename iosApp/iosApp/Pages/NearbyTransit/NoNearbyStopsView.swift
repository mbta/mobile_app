//
//  NoNearbyStopsView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-11-07.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct NoNearbyStopsView: View {
    let onOpenSearch: () -> Void
    let onPanToDefaultCenter: () -> Void

    @EnvironmentObject var settingsCache: SettingsCache

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 16) {
                Image(.mbtaLogo)
                    .resizable()
                    .accessibilityHidden(true)
                    .frame(width: 48, height: 48)
                Text("No nearby stops")
                    .font(Typography.title2Bold)
            }
            .accessibilityAddTraits(.isHeader)
            .accessibilityHeading(.h2)
            Text("You’re outside the MBTA service area.")
            Button(action: onOpenSearch, label: {
                HStack {
                    Text("Search by stop")
                    Image(.faMagnifyingGlassSolid)
                        .resizable()
                        .accessibilityHidden(true)
                        .frame(width: 16, height: 16)
                }
                .foregroundStyle(Color.fill3)
                .padding(8)
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(Color.key)
                .clipShape(.rect(cornerRadius: 8.0))
            })
            if !settingsCache.get(.hideMaps) {
                Button(action: onPanToDefaultCenter, label: {
                    HStack {
                        Text("View transit near Boston")
                        Image(.faMap)
                            .resizable()
                            .accessibilityHidden(true)
                            .frame(width: 16, height: 16)
                    }
                    .foregroundStyle(Color.key)
                    .padding(8)
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .clipShape(.rect(cornerRadius: 8.0))
                    .overlay(RoundedRectangle(cornerRadius: 8.0)
                        .stroke(Color.key, lineWidth: 1.0))
                })
            }
        }
        .font(Typography.body)
        .padding(16)
        .background(Color.fill3)
        .withRoundedBorder(radius: 8)
    }
}

#Preview {
    ZStack {
        NoNearbyStopsView(onOpenSearch: {}, onPanToDefaultCenter: {})
            .padding(16)
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color.fill2)
    .ignoresSafeArea()
}

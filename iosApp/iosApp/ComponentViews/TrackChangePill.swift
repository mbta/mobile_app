//
//  TrackChangePill.swift
//  iosApp
//
//  Created by esimon on 10/19/26.
//  Copyright  2026 MBTA. All rights reserved.
//

import SwiftUI

struct TrackChangePill: View {
    @ObserveInjection var inject

    enum Shape {
        case compact
        case expanded
    }

    var shape: Shape

    private var horizontalPadding: CGFloat {
        switch shape {
        case .compact: 2
        case .expanded: 4
        }
    }

    private var contentSpacing: CGFloat {
        switch shape {
        case .compact: 0
        case .expanded: 8
        }
    }

    private var trackChangeText: Text {
        Text(
            "Track change",
            comment: "Text in an info pill when there is an active track change alert affecting a trip"
        )
    }

    var body: some View {
        HStack(alignment: .center, spacing: contentSpacing) {
            Image(.faShuffle)
                .resizable()
                .frame(width: 16, height: 16)
                .padding(2)
            if shape == .expanded {
                trackChangeText
                    .font(Typography.footnote)
                    .multilineTextAlignment(.leading)
                Image(.faCircleInfo)
                    .resizable()
                    .frame(width: 16, height: 16)
                    .foregroundStyle(Color.deemphasized)
            }
        }
        .foregroundStyle(Color.text)
        .padding(.horizontal, horizontalPadding)
        .padding(.vertical, 2)
        .frame(minWidth: 24, minHeight: 24)
        .background(Color.trackChangePillAlertBackground, in: RoundedRectangle(cornerRadius: 88))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(trackChangeText)
        .enableInjection()
    }
}

#Preview {
    VStack(alignment: .leading, spacing: 16) {
        TrackChangePill(shape: .compact)
        TrackChangePill(shape: .expanded)
    }
    .padding(16)
    .background(Color.fill3)
}

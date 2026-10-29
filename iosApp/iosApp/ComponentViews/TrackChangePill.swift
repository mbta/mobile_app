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

    enum Style {
        case compact(isDimmed: Bool = false)
        case expanded
    }

    enum TrackChangeType {
        case alert
        case scheduled(trackNumber: String, trackLabel: String?)
    }

    var style: Style
    var type: TrackChangeType

    private var horizontalPadding: CGFloat {
        switch style {
        case .compact: 2
        case .expanded: 4
        }
    }

    private var contentSpacing: CGFloat {
        switch style {
        case .compact: 0
        case .expanded: 8
        }
    }

    private var trackChangeText: Text {
        switch type {
        case .alert: Text(
                "Track change",
                comment: "Text in an info pill when there is an active track change alert affecting a trip"
            )
        case let .scheduled(trackNumber, trackLabel): Text(trackLabel ?? trackNumber)
        }
    }

    var backgroundColor: Color {
        switch type {
        case .alert: .trackChangePillAlertBackground
        case let .scheduled(_trackNumber, _trackLabel):
            if case let .compact(isDimmed) = style, isDimmed {
                Color.haloDark
            } else {
                Color.trackChangePillScheduledBackground
            }
        }
    }

    var textColor: Color {
        switch type {
        case .alert: .trackChangePillIcon
        case let .scheduled(_trackNumber, _trackLabel):
            if case let .compact(isDimmed) = style, isDimmed {
                Color.white
            } else {
                Color.trackChangePillIcon
            }
        }
    }

    var body: some View {
        HStack(alignment: .center, spacing: contentSpacing) {
            Image(.faShuffle)
                .resizable()
                .frame(width: 16, height: 16)
                .padding(2)
            if case let .expanded = style {
                trackChangeText
                    .font(Typography.footnote)
                    .multilineTextAlignment(.leading)
                Image(.faCircleInfo)
                    .resizable()
                    .frame(width: 16, height: 16)
                    .foregroundStyle(Color.deemphasized)
            }
        }
        .foregroundStyle(textColor)
        .padding(.horizontal, horizontalPadding)
        .padding(.vertical, 2)
        .frame(minWidth: 24, minHeight: 24)
        .background(backgroundColor, in: RoundedRectangle(cornerRadius: 88))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(trackChangeText)
        .enableInjection()
    }
}

#Preview {
    VStack(alignment: .leading, spacing: 16) {
        TrackChangePill(style: .compact(), type: .alert)
        TrackChangePill(style: .expanded, type: .alert)
    }
    .padding(16)
    .background(Color.fill3)
}

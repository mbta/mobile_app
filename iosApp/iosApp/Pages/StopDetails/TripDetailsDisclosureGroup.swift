//
//  TripDetailsDisclosureGroup.swift
//  iosApp
//
//  Created by esimon on 12/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct TripDetailsDisclosureGroup: DisclosureGroupStyle {
    @State var caretRotation: Angle = .zero
    func makeBody(configuration: Configuration) -> some View {
        VStack(spacing: 0) {
            Button(
                action: { withAnimation { configuration.isExpanded.toggle() } },
                label: {
                    HStack(spacing: 0) {
                        Image(.faCaretRight)
                            .resizable()
                            .frame(width: 6, height: 10)
                            .rotationEffect(caretRotation)
                            .foregroundStyle(Color.deemphasized)
                            .frame(width: 24, height: 24)
                            .padding(.leading, 8)
                        configuration.label
                    }.frame(maxWidth: .infinity)
                }
            ).onChange(of: configuration.isExpanded) { expanded in
                withAnimation(.easeInOut(duration: 0.2)) {
                    caretRotation = expanded ? .degrees(90) : .zero
                }
            }
            configuration.content
                .frame(height: configuration.isExpanded ? nil : 0, alignment: .top)
                .clipped()
                .accessibilityHidden(!configuration.isExpanded)
        }
    }
}

extension DisclosureGroupStyle where Self == TripDetailsDisclosureGroup {
    static var tripDetails: TripDetailsDisclosureGroup { .init() }
}

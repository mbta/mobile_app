//
//  WithRealtimeIndicator.swift
//  iosApp
//
//  Created by esimon on 10/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct WithRealtimeIndicator: View {
    private static let subjectSpacing: CGFloat = 4
    @ScaledMetric private var iconSize: CGFloat = 12
    let prediction: any View
    let visible: Bool

    init(_ prediction: () -> any View, visible: Bool = true) {
        self.prediction = prediction()
        self.visible = visible
    }

    init(_ prediction: any View, visible: Bool = true) {
        self.prediction = prediction
        self.visible = visible
    }

    var body: some View {
        HStack(spacing: Self.subjectSpacing) {
            if visible {
                Image(.liveData)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: iconSize, height: iconSize)
                    .padding(4)
                    .opacity(0.6)
                    .accessibilityHidden(true)
            }
            AnyView(prediction)
        }
    }
}

struct WithRealtimeIndicatorModifier: ViewModifier {
    var visible: Bool = true
    func body(content: Content) -> some View {
        WithRealtimeIndicator(content, visible: visible)
    }
}

extension View {
    func realtime(visible: Bool = true) -> some View {
        modifier(WithRealtimeIndicatorModifier(visible: visible))
    }
}

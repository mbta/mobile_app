//
//  RealtimeIndicator.swift
//  iosApp
//
//  Created by esimon on 10/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct RealtimeIndicator: View {
    private static let subjectSpacing: CGFloat = 4
    @ScaledMetric private var iconSize: CGFloat = 12
    let prediction: any View

    init(_ prediction: () -> any View) {
        self.prediction = prediction()
    }

    init(_ prediction: any View) {
        self.prediction = prediction
    }

    var body: some View {
        HStack(spacing: Self.subjectSpacing) {
            Image(.liveData)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: iconSize, height: iconSize)
                .padding(4)
                .foregroundStyle(Color.text)
                .opacity(0.6)
                .accessibilityHidden(true)
            AnyView(prediction)
        }
    }
}

struct RealtimeIndicatorModifier: ViewModifier {
    func body(content: Content) -> some View {
        RealtimeIndicator(content)
    }
}

extension View {
    func realtime() -> some View {
        modifier(RealtimeIndicatorModifier())
    }
}

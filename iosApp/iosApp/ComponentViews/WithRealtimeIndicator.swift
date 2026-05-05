//
//  WithRealtimeIndicator.swift
//  iosApp
//
//  Created by esimon on 10/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct WithRealtimeIndicator: View {
    @ObserveInjection var inject
    private static let subjectSpacing: CGFloat = 4
    @ScaledMetric private var iconSize: CGFloat = 12
    let prediction: any View
    let hideIndicator: Bool

    init(_ prediction: () -> any View, hideIndicator: Bool = false) {
        self.prediction = prediction()
        self.hideIndicator = hideIndicator
    }

    init(_ prediction: any View, hideIndicator: Bool = false) {
        self.prediction = prediction
        self.hideIndicator = hideIndicator
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: Self.subjectSpacing) {
            if !hideIndicator {
                Image(.liveData)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: iconSize, height: iconSize)
                    .padding(.bottom, -1)
                    .opacity(0.6)
                    .accessibilityHidden(true)
            }
            AnyView(prediction)
        }
        .enableInjection()
    }
}

struct WithRealtimeIndicatorModifier: ViewModifier {
    var hideIndicator: Bool = false
    func body(content: Content) -> some View {
        WithRealtimeIndicator(content, hideIndicator: hideIndicator)
            .enableInjection()
    }
}

extension View {
    func realtime(hideIndicator: Bool = false) -> some View {
        modifier(WithRealtimeIndicatorModifier(hideIndicator: hideIndicator))
    }
}

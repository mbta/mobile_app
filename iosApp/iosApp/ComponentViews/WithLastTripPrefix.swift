//
//  WithLastTripPrefix.swift
//  iosApp
//
//  Created by esimon on 1/12/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import SwiftUI

struct WithLastTripPrefix: View {
    private static let subjectSpacing: CGFloat = 4
    @ScaledMetric private var iconSize: CGFloat = 12
    let content: any View
    let last: Bool
    let scheduleClock: Bool

    init(_ content: () -> any View, last: Bool, scheduleClock: Bool = false) {
        self.content = content()
        self.last = last
        self.scheduleClock = scheduleClock
    }

    init(_ content: any View, last: Bool, scheduleClock: Bool = false) {
        self.content = content
        self.last = last
        self.scheduleClock = scheduleClock
    }

    var body: some View {
        HStack(alignment: .center, spacing: Self.subjectSpacing) {
            if last {
                if scheduleClock {
                    Image(.faClock)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: iconSize, height: iconSize)
                        .padding(.trailing, 2)
                        .accessibilityHidden(true)
                }
                HStack(alignment: .firstTextBaseline, spacing: Self.subjectSpacing) {
                    Text("Last",
                         comment: """
                         A prefix included before the prediction or schedule for the final trip of the day.
                         This is used in areas with minimal horizontal space,
                         so it must be limited to as few characters as possible.
                         """).font(.footnote).accessibilityHidden(true)
                    AnyView(content)
                }
            } else {
                AnyView(content)
            }
        }
    }
}

struct WithLastTripPrefixModifier: ViewModifier {
    var last: Bool
    var scheduleClock: Bool = false
    func body(content: Content) -> some View {
        WithLastTripPrefix(content, last: last, scheduleClock: scheduleClock)
    }
}

extension View {
    func lastTripPrefix(last: Bool, scheduleClock: Bool = false) -> some View {
        modifier(WithLastTripPrefixModifier(last: last, scheduleClock: scheduleClock))
    }
}

//
//  HaloScrollView.swift
//  iosApp
//
//  Created by esimon on 7/31/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

struct ScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value += nextValue()
    }
}

struct HaloScrollView<Content>: View where Content: View {
    var content: Content
    var axes: Axis.Set

    var haloColor: Color
    var haloHeight: CGFloat
    var alwaysShowHalo: Bool

    init(
        _ axes: Axis.Set = .vertical,
        haloColor: Color = .halo,
        haloHeight: CGFloat = 2,
        alwaysShowHalo: Bool = false,
        @ViewBuilder content: () -> Content
    ) {
        self.axes = axes
        self.haloColor = haloColor
        self.haloHeight = haloHeight
        self.alwaysShowHalo = alwaysShowHalo
        self.content = content()
    }

    @State private var haloVisible = false

    @MainActor @preconcurrency var body: some View {
        ZStack(alignment: .top) {
            ScrollView(axes) {
                content
                    .background(
                        GeometryReader { inner in
                            // Invisible background to programmatically check the scroll position
                            // Unfortunately there's no better way to do this until iOS 18
                            Color.clear.preference(
                                key: ScrollOffsetPreferenceKey.self,
                                value: inner.frame(in: .named("scroll")).origin.y
                            )
                        }
                    )
            }
            .coordinateSpace(name: "scroll")
            .onPreferenceChange(ScrollOffsetPreferenceKey.self) { value in
                if haloVisible, value == 0 {
                    withAnimation {
                        haloVisible = false
                    }
                } else if !haloVisible, value != 0 {
                    withAnimation {
                        haloVisible = true
                    }
                }
            }
            if haloVisible || alwaysShowHalo {
                HaloSeparator(height: haloHeight, haloColor: haloColor).transition(.opacity)
            }
        }
    }
}

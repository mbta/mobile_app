//
//  LoadingPlaceholderModifier.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-10-23.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shimmer
import SwiftUI

struct LoadingPlaceholderModifier: ViewModifier {
    let withShimmer: Bool
    func body(content: Content) -> some View {
        content
            .redacted(reason: .placeholder)
            .shimmering(active: withShimmer)
            .allowsHitTesting(false)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Loading...")
    }
}

extension View {
    func loadingPlaceholder(_ withShimmer: Bool = true) -> some View {
        modifier(LoadingPlaceholderModifier(withShimmer: withShimmer))
    }
}

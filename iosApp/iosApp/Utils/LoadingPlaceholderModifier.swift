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
    func body(content: Content) -> some View {
        content
            .redacted(reason: .placeholder)
            .shimmering()
            .allowsHitTesting(false)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Loading...")
    }
}

extension View {
    func loadingPlaceholder() -> some View {
        modifier(LoadingPlaceholderModifier())
    }
}

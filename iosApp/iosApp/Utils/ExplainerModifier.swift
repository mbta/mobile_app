//
//  ExplainerModifier.swift
//  iosApp
//
//  Created by Simon, Emma on 8/2/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

struct ExplainerModifier: ViewModifier {
    @Binding var explainer: Explainer?

    func body(content: Content) -> some View {
        content.fullScreenCover(
            isPresented: .init(
                get: { explainer != nil },
                set: { value in if !value { explainer = nil } }
            )
        ) {
            if let displayedExplainer = explainer {
                ExplainerPage(
                    explainer: displayedExplainer,
                    onClose: { explainer = nil }
                )
            }
        }
    }
}

public extension View {
    func explainer(_ explainer: Binding<Explainer?>) -> some View {
        modifier(ExplainerModifier(explainer: explainer))
    }
}

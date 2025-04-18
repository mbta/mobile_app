//
//  RoundedBorderModifier.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct RoundedBorderModifier: ViewModifier {
    var radius: CGFloat = 8
    var color: Color = .halo
    var opacity: CGFloat = 1
    var width: CGFloat = 1

    func body(content: Content) -> some View {
        content
            .clipShape(RoundedRectangle(cornerRadius: radius))
            .background(RoundedRectangle(cornerRadius: radius)
                // Strokes are drawn centered on the component border,
                // so the width is doubled to get the visible width to match the desired value.
                .stroke(color.opacity(opacity), lineWidth: width * 2))
    }
}

public extension View {
    func withRoundedBorder(
        radius: CGFloat = 8,
        color: Color? = nil,
        opacity: CGFloat = 1,
        width: CGFloat = 1
    ) -> some View {
        if let color {
            return modifier(RoundedBorderModifier(radius: radius, color: color, opacity: opacity, width: width))
        }
        return modifier(RoundedBorderModifier(radius: radius, opacity: opacity, width: width))
    }
}

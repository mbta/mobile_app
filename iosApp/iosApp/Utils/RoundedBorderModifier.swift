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
    var opacity: CGFloat = 0.1
    var width: CGFloat = 1

    func body(content: Content) -> some View {
        content
            .clipShape(.rect(cornerRadius: radius))
            .border(color.opacity(opacity), width: width)
    }
}

public extension View {
    func withRoundedBorder(
        radius: CGFloat = 8,
        color: Color? = nil,
        opacity: CGFloat = 0.1,
        width: CGFloat = 1
    ) -> some View {
        if let color {
            return modifier(RoundedBorderModifier(radius: radius, color: color, opacity: opacity, width: width))
        }
        return modifier(RoundedBorderModifier(radius: radius, opacity: opacity, width: width))
    }
}

//
//  UnevenRoundedBorderModifier.swift
//  iosApp
//
//  Created by Brady, Kayla on 3/24/26.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct UnevenRoundedBorderModifier: ViewModifier {
    var topRadius: CGFloat = 8
    var bottomRadius: CGFloat = 8
    var color: Color = .halo
    var opacity: CGFloat = 1
    var width: CGFloat = 1

    func body(content: Content) -> some View {
        content
            .clipShape(UnevenRoundedRectangle(
                topLeadingRadius: topRadius,
                bottomLeadingRadius: bottomRadius,
                bottomTrailingRadius: bottomRadius,
                topTrailingRadius: topRadius,
                style: .circular
            ))
            .background(UnevenRoundedRectangle(
                topLeadingRadius: topRadius,
                bottomLeadingRadius: bottomRadius,
                bottomTrailingRadius: bottomRadius,
                topTrailingRadius: topRadius,
                style: .circular
            )
            // Strokes are drawn centered on the component border,
            // so the width is doubled to get the visible width to match the desired value.
            .stroke(color.opacity(opacity), lineWidth: width * 2))
    }
}

public extension View {
    func withUnevenRoundedBorder(
        topRadius: CGFloat = 8,
        bottomRadius: CGFloat = 8,
        color: Color? = nil,
        opacity: CGFloat = 1,
        width: CGFloat = 1
    ) -> some View {
        modifier(UnevenRoundedBorderModifier(topRadius: topRadius,
                                             bottomRadius: bottomRadius,
                                             color: color ?? .halo,
                                             opacity: opacity,
                                             width: width))
    }
}

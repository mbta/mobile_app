//
//  FullWidthButtonModifiers.swift
//  iosApp
//
//  Created by esimon on 10/29/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct KeyButton: ViewModifier {
    func body(content: Content) -> some View {
        content
            .foregroundStyle(Color.fill3)
            .font(Typography.bodySemibold)
            .padding(8)
            .frame(maxWidth: .infinity, minHeight: 52)
            .background(Color.key)
            .clipShape(.rect(cornerRadius: 8.0))
    }
}

struct SecondaryButton: ViewModifier {
    func body(content: Content) -> some View {
        content
            .foregroundStyle(Color.key)
            .padding(8)
            .frame(maxWidth: .infinity, minHeight: 52)
            .clipShape(.rect(cornerRadius: 8.0))
            .overlay(RoundedRectangle(cornerRadius: 8.0)
                .stroke(Color.key, lineWidth: 1.0))
    }
}

extension View {
    func fullWidthKeyButton() -> some View {
        modifier(KeyButton())
    }

    func fullWidthSecondaryButton() -> some View {
        modifier(SecondaryButton())
    }
}

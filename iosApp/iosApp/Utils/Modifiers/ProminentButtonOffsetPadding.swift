//
//  ProminentButtonOffsetPadding.swift
//  iosApp
//
//  Created by Kayla Brady on 5/8/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import SwiftUI

struct RemoveProminentButtonPadding: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(.horizontal, -10)
            .padding(.vertical, -8)
    }
}

public extension View {
    /** Offsets the padding that is built into the prominent button style so we can calculate our own padding */
    func withProminentButtonPaddingRemoved(
    ) -> some View {
        modifier(RemoveProminentButtonPadding())
    }
}

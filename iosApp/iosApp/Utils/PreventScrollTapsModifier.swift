//
//  PreventScrollTapsModifier.swift
//  iosApp
//
//  Created by esimon on 8/11/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

struct PreventScrollTapsModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .simultaneousGesture(TapGesture())
    }
}

extension View {
    /**
     Prevent tap gestures during scrolling, so that buttons are not triggered accidentally while
     the user is trying to scroll in a list or resize the sheet.
     */
    func preventScrollTaps() -> some View {
        modifier(PreventScrollTapsModifier())
    }
}

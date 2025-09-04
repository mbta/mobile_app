//
//  ViewModifier.swift
//  iosApp
//
//  Created by Melody Horn on 4/7/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

public extension View {
    /// Sets this view’s focus size to be the entire size of the view including any preceding `.frame()` calls.
    func fullFocusSize() -> some View {
        contentShape(.rect)
    }
}

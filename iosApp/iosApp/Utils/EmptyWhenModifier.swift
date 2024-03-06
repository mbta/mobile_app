//
//  EmptyWhenModifier.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct EmptyWhenModifier: ViewModifier {
    var isEmpty: Bool

    func body(content: Content) -> some View {
        if isEmpty {
            EmptyView()
        } else {
            content
        }
    }
}

public extension View {
    func emptyWhen(_ isEmpty: Bool) -> some View {
        modifier(EmptyWhenModifier(isEmpty: isEmpty))
    }
}

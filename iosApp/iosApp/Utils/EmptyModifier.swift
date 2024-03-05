//
//  EmptyModifier.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct EmptyModifier: ViewModifier {
    var isEmpty: Bool

    func body(content: Content) -> some View {
        if isEmpty {
            EmptyView()
        } else {
            content
        }
    }
}

extension View {
    func emptyWhen(_ isEmpty: Bool) -> some View {
        modifier(EmptyModifier(isEmpty: isEmpty))
    }
}

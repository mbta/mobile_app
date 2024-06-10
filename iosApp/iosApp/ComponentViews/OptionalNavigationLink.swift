//
//  OptionalNavigationLink.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

/// In the filtered stop details state, non-vehicle-based upcoming trips should not link to the trip details page.
/// This will render a button if there there is a SheetNavigationStackEntry to navigate to, otherwise it renders a
/// regular label.
struct OptionalNavigationLink<Label>: View where Label: View {
    let value: SheetNavigationStackEntry?
    let action: (SheetNavigationStackEntry) -> Void
    let label: () -> Label

    var body: some View {
        if let value {
            SheetNavigationLink(value: value, action: action, label: label)
        } else {
            label()
        }
    }
}

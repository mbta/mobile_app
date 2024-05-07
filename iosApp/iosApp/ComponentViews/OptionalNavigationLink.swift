//
//  OptionalNavigationLink.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

/// `NavigationLink` takes an optional `value` but renders as a disabled link when the `value` is `nil`.
/// In the filtered stop details state, non-vehicle-based upcoming trips should not render as disabled links.
/// This will instead render its label directly if the `value` is `nil`, bypassing the link completely.
struct OptionalNavigationLink<Label>: View where Label: View {
    let value: SheetNavigationStackEntry?
    let label: () -> Label

    var body: some View {
        if let value {
            NavigationLink(value: value, label: label)
        } else {
            label()
        }
    }
}

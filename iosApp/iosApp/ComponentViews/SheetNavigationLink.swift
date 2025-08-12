//
//  SheetNavigationLink.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/10/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct SheetNavigationLink<Label>: View where Label: View {
    let value: SheetNavigationStackEntry
    let action: (SheetNavigationStackEntry) -> Void
    let showChevron: Bool
    let label: () -> Label

    @ScaledMetric private var chevronHeight: CGFloat = 14
    @ScaledMetric private var chevronWidth: CGFloat = 8

    var body: some View {
        Button(action: { action(value) }) {
            HStack(spacing: 0) {
                label()
                if showChevron {
                    Image(.faChevronRight)
                        .resizable()
                        .scaledToFit()
                        .frame(width: chevronWidth, height: chevronHeight)
                        .padding(5)
                        .foregroundStyle(Color.deemphasized)
                }
            }
        }
        .preventScrollTaps()
    }
}

//
//  PlainDisclosureGroupStyle.swift
//  iosApp
//  A basic DisclosureGroupStyle that removes the default expanded arrow.
//
//  Created by esimon on 2/18/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import SwiftUI

struct PlainDisclosureGroupStyle: DisclosureGroupStyle {
    func makeBody(configuration: Configuration) -> some View {
        VStack(spacing: 0) {
            Button(
                action: { withAnimation { configuration.isExpanded.toggle() } },
                label: { configuration.label }
            ).foregroundStyle(Color.text)
            if configuration.isExpanded {
                configuration.content
            }
        }
    }
}

//
//  LineSection.swift
//  iosApp
//
//  Created by Simon, Emma on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct LineSection<Content: View>: View {
    let line: Line
    let routes: [Route]
    let pinned: Bool
    let onPin: (String) -> Void
    let content: () -> Content

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    var body: some View {
        TransitSection(header: {
            LineHeader(line: line, routes: routes) {
                PinButton(pinned: pinned, action: { onPin(line.id) })
            }
        }, content: content)
    }
}

//
//  LineHeader.swift
//  iosApp
//
//  Created by Simon, Emma on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

struct LineHeader<Content: View>: View {
    let line: Line
    let routes: [Route]
    var rightContent: () -> Content?

    init(line: Line, routes: [Route], @ViewBuilder rightContent: @escaping () -> Content? = { EmptyView() }) {
        self.line = line
        self.routes = routes
        self.rightContent = rightContent
    }

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    var body: some View {
        if let route = routes.first {
            TransitHeader(
                name: line.longName,
                routeType: route.type,
                backgroundColor: Color(hex: line.color),
                textColor: Color(hex: line.textColor),
                rightContent: rightContent
            )
            .accessibilityElement(children: .contain)
        } else {
            EmptyView()
        }
    }
}

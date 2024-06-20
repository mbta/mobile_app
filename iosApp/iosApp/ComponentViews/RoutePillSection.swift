//
//  RoutePillSection.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct RoutePillSection<Content: View>: View {
    let route: Route
    let line: Line?
    let headerContent: any View
    let content: () -> Content

    init(route: Route, line: Line?, headerContent: (any View)? = nil, content: @escaping () -> Content) {
        self.route = route
        self.line = line
        self.headerContent = headerContent ?? EmptyView()
        self.content = content
    }

    var body: some View {
        Section(content: content, header: {
            VStack(alignment: .leading) {
                RoutePill(route: route, line: line, type: .flex).padding(.leading, -20)
                AnyView(headerContent)
            }
        })
    }
}

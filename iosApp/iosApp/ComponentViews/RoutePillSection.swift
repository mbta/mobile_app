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
    let directionPicker: any View
    let content: () -> Content

    init(route: Route, directionPicker: (any View)? = nil, content: @escaping () -> Content) {
        self.route = route
        self.directionPicker = directionPicker ?? EmptyView()
        self.content = content
    }

    var body: some View {
        Section(content: content, header: {
            VStack(alignment: .leading) {
                RoutePill(route: route).padding(.leading, -20)
                AnyView(directionPicker)
            }
        })
    }
}

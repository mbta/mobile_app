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
    let content: () -> Content

    var body: some View {
        Section(content: content, header: { RoutePill(route: route).padding(.leading, -20) })
    }
}

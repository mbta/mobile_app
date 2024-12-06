//
//  RouteLine.swift
//  iosApp
//
//  Created by esimon on 12/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct RouteLine: View {
    var color: Color

    init(_ color: Color) {
        self.color = color
    }

    var body: some View {
        Rectangle()
            .frame(minWidth: 4, maxWidth: 4)
            .foregroundStyle(color)
    }
}

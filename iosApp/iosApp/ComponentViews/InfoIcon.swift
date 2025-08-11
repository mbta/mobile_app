//
//  InfoIcon.swift
//  iosApp
//
//  Created by esimon on 1/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

struct InfoIcon: View {
    var size: Double = 16

    var body: some View {
        Image(.faCircleInfo)
            .resizable()
            .frame(width: size, height: size)
            .foregroundStyle(Color.translucentContrast)
            .accessibilityHidden(true)
    }
}

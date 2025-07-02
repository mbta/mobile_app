//
//  HaloSeparator.swift
//  iosApp
//
//  Created by esimon on 12/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct HaloSeparator: View {
    var height: CGFloat = 1

    var body: some View {
        Color.halo.frame(height: height)
    }
}

struct VerticalHaloSeparator: View {
    var width: CGFloat = 1

    var body: some View {
        Color.halo.frame(width: width)
    }
}

//
//  TransitSection.swift
//  iosApp
//
//  Created by Simon, Emma on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct TransitSection<Header: View, Content: View>: View {
    let header: () -> Header
    let content: () -> Content

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    var body: some View {
        VStack(spacing: 0) {
            header()
            content()
        }
        .background(Color.fill3)
        .withRoundedBorder()
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }
}

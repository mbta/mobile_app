//
//  TransitCard.swift
//  iosApp
//
//  Created by Simon, Emma on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct TransitCard<Header: View, Content: View>: View {
    let header: () -> Header
    let content: () -> Content

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    var body: some View {
        VStack(spacing: 0) {
            header()
            content()
        }
        .background(Color.fill3)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.halo, lineWidth: 1)
        )
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }
}

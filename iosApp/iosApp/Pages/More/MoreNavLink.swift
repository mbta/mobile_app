//
//  MoreNavLink.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-11-01.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct MoreNavLink: View {
    var label: String
    var destination: MoreNavTarget

    @ScaledMetric
    private var iconSize: CGFloat = 10.5

    var body: some View {
        NavigationLink(value: destination) {
            HStack(alignment: .center, spacing: 0) {
                Text(label)
                    .foregroundStyle(Color.text)
                    .font(Typography.body)
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(Color.deemphasized)
                    .fontWeight(.bold)
            }
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 16)
        .frame(minHeight: 44)
    }
}

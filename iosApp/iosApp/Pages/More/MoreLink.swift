//
//  MoreLink.swift
//  iosApp
//
//  Created by esimon on 10/28/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct MoreLink: View {
    var label: String
    var url: String
    var note: String? = nil

    @ScaledMetric
    private var iconSize: CGFloat = 10.5

    var body: some View {
        Link(destination: URL(string: url)!) {
            HStack(alignment: .center, spacing: 0) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(label)
                        .foregroundStyle(Color.text)
                        .font(Typography.body)
                    if let note {
                        Text(note)
                            .foregroundStyle(Color.text)
                            .font(Typography.footnote)
                            .opacity(0.6)
                            .padding(.top, 2)
                    }
                }
                Spacer()
                Image(systemName: "arrow.up.right")
                    .resizable()
                    .frame(width: iconSize, height: iconSize, alignment: .center)
                    .foregroundStyle(Color.deemphasized)
                    .fontWeight(.bold)
            }
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 16)
        .frame(minHeight: 44)
    }
}

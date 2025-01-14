//
//  StopDetailsIconCard.swift
//  iosApp
//
//  Created by esimon on 1/7/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct StopDetailsIconCard<Header: View, Details: View>: View {
    var accentColor: Color
    var details: Details?
    var header: Header
    var icon: Image

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 16) {
                icon
                    .resizable().scaledToFit()
                    .frame(width: 35, height: 35)
                    .frame(width: 48, height: 48)
                    .foregroundStyle(accentColor)
                    .accessibilityHidden(true)
                header
                    .font(Typography.title2Bold)
                    .foregroundStyle(Color.text)
                    .accessibilityAddTraits(.isHeader)
            }.frame(maxWidth: .infinity, alignment: .leading)

            if let details {
                Divider().frame(height: 2).background(accentColor.opacity(0.25))
                details.font(Typography.callout)
            }
        }
        .padding(16)
        .background(Color.fill3)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.halo, lineWidth: 1))
        .padding(.horizontal, 16)
    }
}

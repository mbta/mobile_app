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
    var details: Details?
    var header: Header
    var headerColor: Color
    var icon: Image

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 16) {
                icon
                    .resizable().scaledToFit()
                    .frame(width: 35, height: 35)
                    .foregroundStyle(headerColor)
                    .frame(width: 48, height: 48)
                header.font(Typography.title2Bold).foregroundStyle(headerColor)
            }.frame(maxWidth: .infinity, alignment: .leading)

            if let details {
                HaloSeparator()
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

//
//  MorePhone.swift
//  iosApp
//
//  Created by esimon on 10/28/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct MorePhone: View {
    var label: String
    var phoneNumber: String

    @ScaledMetric
    private var iconSize: CGFloat = 16

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                Text(label)
                    .foregroundStyle(Color.text)
                    .font(Typography.body)
            }
            Spacer()
            Image(.faPhone)
                .resizable()
                .frame(width: iconSize, height: iconSize, alignment: .center)
                .foregroundStyle(Color.deemphasized)
                .fontWeight(.semibold)
                .accessibilityHidden(true)
        }
        .contentShape(Rectangle())
        .padding(.vertical, 10)
        .padding(.horizontal, 16)
        .frame(minHeight: 44)
        .onTapGesture {
            if let url = URL(string: "tel://\(phoneNumber)"), UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url)
            }
        }
        .accessibilityAddTraits(.isButton)
        .accessibilityHint(Text("Select to call", comment: "Screen reader text for a link to call a phone number"))
    }
}

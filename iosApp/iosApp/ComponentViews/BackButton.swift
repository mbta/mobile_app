//
//  BackButton.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct BackButton: View {
    let onPress: () -> Void

    @ScaledMetric private var chevronHeight: CGFloat = 14
    @ScaledMetric private var chevronWidth: CGFloat = 8

    @ScaledMetric private var circleWidth: CGFloat = 24
    @ScaledMetric private var tapWidth: CGFloat = 32

    var body: some View {
        Button(action: { onPress() }) {
            ZStack {
                Circle()
                    .fill(Color.primary)
                    .frame(width: circleWidth, height: circleWidth)

                Image(.faChevronLeft)
                    .resizable()
                    .scaledToFit()
                    .frame(width: chevronWidth, height: chevronHeight)
                    .padding(5)
                    .foregroundStyle(Color.fill3)
            }
        }
        .frame(width: tapWidth, height: tapWidth)
        .accessibilityLabel("Back")
    }
}

struct BackButton_Previews: PreviewProvider {
    static var previews: some View {
        BackButton(onPress: { print("Pressed") }).previewDisplayName("Back Button")
    }
}

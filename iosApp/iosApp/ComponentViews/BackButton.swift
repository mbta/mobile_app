//
//  BackButton.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/17/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct BackButton: View {
    let action: () -> Void

    @ScaledMetric private var chevronSize: CGFloat = 14

    @ScaledMetric private var circleSize: CGFloat = ActionButtonProperties.circleSize
    @ScaledMetric private var tapSize: CGFloat = ActionButtonProperties.tapSize

    var body: some View {
        Button(action: { action() }) {
            ZStack {
                Circle()
                    .fill(Color.contrast)
                    .frame(width: circleSize, height: circleSize)

                Image(.faChevronLeft)
                    .resizable()
                    .scaledToFit()
                    .frame(width: chevronSize, height: chevronSize)
                    .padding(5)
                    .foregroundStyle(Color.fill2)
            }
        }
        .frame(width: circleSize, height: circleSize)
        .accessibilityLabel("Back")
    }
}

struct BackButton_Previews: PreviewProvider {
    static var previews: some View {
        BackButton(action: { print("Pressed") }).previewDisplayName("Back Button")
    }
}

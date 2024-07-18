//
//  CloseButton.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct CloseButton: View {
    let action: () -> Void

    @ScaledMetric private var xSize: CGFloat = 10

    @ScaledMetric private var circleSize: CGFloat = 32
    @ScaledMetric private var tapSize: CGFloat = 32

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(Color.contrast)
                    .frame(width: circleSize, height: circleSize)

                Image(.faXmark)
                    .resizable()
                    .scaledToFit()
                    .frame(width: xSize, height: xSize)
                    .padding(5)
                    .foregroundStyle(Color.fill2)
            }
        }
        .frame(width: circleSize, height: circleSize)
        .accessibilityLabel("Close")
    }
}

struct CloseButton_Previews: PreviewProvider {
    static var previews: some View {
        CloseButton(action: { print("Pressed") }).previewDisplayName("Close Button")
    }
}

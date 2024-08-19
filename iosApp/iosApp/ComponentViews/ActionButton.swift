//
//  ActionButton.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct ActionButton: View {
    enum Kind {
        case back
        case close

        var accessibilityLabel: String {
            switch self {
            case .back:
                "Back"
            case .close:
                "Close"
            }
        }

        var image: ImageResource {
            switch self {
            case .back:
                .faChevronLeft
            case .close:
                .faXmark
            }
        }
    }

    let kind: Kind
    let action: () -> Void

    @ScaledMetric private var circleSize: CGFloat = 32

    @ScaledMetric private var backIconSize: CGFloat = 14
    @ScaledMetric private var closeIconSize: CGFloat = 10

    var iconSize: CGFloat {
        switch kind {
        case .back:
            backIconSize
        case .close:
            closeIconSize
        }
    }

    var body: some View {
        Button(action: { action() }) {
            ZStack {
                Circle()
                    .fill(Color.contrast)
                    .frame(width: circleSize, height: circleSize)

                Image(kind.image)
                    .resizable()
                    .scaledToFit()
                    .frame(width: iconSize, height: iconSize)
                    .padding(5)
                    .foregroundStyle(Color.fill2)
            }
        }
        .frame(width: circleSize, height: circleSize)
        .accessibilityLabel(kind.accessibilityLabel)
    }
}

struct ActionButton_Previews: PreviewProvider {
    static var previews: some View {
        ActionButton(kind: .back, action: { print("Pressed") }).previewDisplayName("Back Button")
        ActionButton(kind: .close, action: { print("Pressed") }).previewDisplayName("Close Button")
    }
}

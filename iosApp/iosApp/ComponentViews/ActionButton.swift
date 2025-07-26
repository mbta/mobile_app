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
        case clear

        var accessibilityLabel: String {
            switch self {
            case .back:
                NSLocalizedString("Back", comment: "VoiceOver label for a generic back button")
            case .close:
                NSLocalizedString("Close", comment: "VoiceOver label for a generic close button")
            case .clear:
                NSLocalizedString("Clear", comment: "VoiceOver label for a generic clear button")
            }
        }

        var image: ImageResource {
            switch self {
            case .back:
                .faChevronLeft
            case .close, .clear:
                .faXmark
            }
        }
    }

    let kind: Kind
    var circleColorOverride: Color? = nil
    var iconColorOverride: Color? = nil
    let action: () -> Void

    @ScaledMetric private var circleSize: CGFloat = 32

    @ScaledMetric private var backIconSize: CGFloat = 14
    @ScaledMetric private var closeIconSize: CGFloat = 12

    var iconSize: CGFloat {
        switch kind {
        case .back:
            backIconSize
        case .close:
            closeIconSize
        case .clear:
            closeIconSize / 2
        }
    }

    var circleColor: Color {
        if let circleColorOverride {
            circleColorOverride
        } else {
            switch kind {
            case .back, .close:
                Color.contrast
            case .clear:
                Color.deemphasized
            }
        }
    }

    var iconColor: Color {
        if let iconColorOverride {
            iconColorOverride
        } else {
            switch kind {
            case .back, .close:
                Color.fill2
            case .clear:
                Color.fill3
            }
        }
    }

    var body: some View {
        let buttonSize = kind == .clear ? circleSize / 2 : circleSize
        Button(action: { action() }) {
            ZStack {
                Circle()
                    .fill(circleColor)
                    .frame(width: buttonSize, height: buttonSize)

                Image(kind.image)
                    .resizable()
                    .scaledToFit()
                    .frame(width: iconSize, height: iconSize)
                    .foregroundStyle(iconColor)
            }
        }
        .frame(width: buttonSize, height: buttonSize)
        .accessibilityLabel(kind.accessibilityLabel)
    }
}

struct ActionButton_Previews: PreviewProvider {
    static var previews: some View {
        ActionButton(kind: .back, action: { print("Pressed") }).previewDisplayName("Back Button")
        ActionButton(kind: .close, action: { print("Pressed") }).previewDisplayName("Close Button")
        ActionButton(kind: .clear, action: { print("Pressed") }).previewDisplayName("Clear Button")
    }
}

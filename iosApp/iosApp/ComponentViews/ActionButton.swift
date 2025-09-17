//
//  ActionButton.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct ActionButton: View {
    enum Kind {
        case back
        case close
        case clear
        case dismiss
        case plus

        var accessibilityLabel: String {
            switch self {
            case .back:
                NSLocalizedString("Back", comment: "VoiceOver label for a generic back button")
            case .close:
                NSLocalizedString("Close", comment: "VoiceOver label for a generic close button")
            case .clear:
                NSLocalizedString("Clear", comment: "VoiceOver label for a generic clear button")
            case .dismiss:
                NSLocalizedString("Dismiss", comment: "VoiceOver label for a dismiss  button")
            case .plus:
                NSLocalizedString("Add stops", comment: "VoiceOver label for add stops button")
            }
        }

        var image: ImageResource {
            switch self {
            case .back:
                .faChevronLeft
            case .close, .clear, .dismiss:
                .faXmark
            case .plus:
                .plus
            }
        }
    }

    private let kind: Kind
    private let circleColor: Color
    private let iconColor: Color
    private let action: () -> Void

    @ScaledMetric private var circleSize: CGFloat = 32
    @ScaledMetric private var backIconSize: CGFloat = 14
    @ScaledMetric private var closeIconSize: CGFloat = 12
    @ScaledMetric private var plusIconSize: CGFloat = 13

    var iconSize: CGFloat {
        switch kind {
        case .back:
            backIconSize
        case .close, .dismiss:
            closeIconSize
        case .clear:
            closeIconSize / 2
        case .plus:
            plusIconSize
        }
    }

    init(
        kind: ActionButton.Kind,
        circleColor: Color? = nil,
        iconColor: Color? = nil,
        action: @escaping () -> Void
    ) {
        self.kind = kind
        self.circleColor = if let circleColor {
            circleColor
        } else {
            switch kind {
            case .back, .close, .dismiss, .plus:
                Color.contrast
            case .clear:
                Color.deemphasized
            }
        }
        self.iconColor = if let iconColor {
            iconColor
        } else {
            switch kind {
            case .back, .close, .dismiss, .plus:
                Color.fill2
            case .clear:
                Color.fill3
            }
        }
        self.action = action
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

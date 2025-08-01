//
//  SheetHeader.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/10/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct SheetHeader<Content: View>: View {
    let title: String?
    let titleColor: Color
    let buttonColor: Color
    let buttonTextColor: Color
    let onBack: (() -> Void)?
    let onClose: (() -> Void)?
    let closeText: String?
    let rightActionContents: () -> Content

    init(
        title: String?,
        titleColor: Color = Color.text,
        buttonColor: Color = Color.contrast,
        buttonTextColor: Color = Color.fill2,
        onBack: (() -> Void)? = nil,
        onClose: (() -> Void)? = nil,
        closeText: String? = nil,
        @ViewBuilder rightActionContents: @escaping () -> Content = { EmptyView() }
    ) {
        self.title = title
        self.titleColor = titleColor
        self.buttonColor = buttonColor
        self.buttonTextColor = buttonTextColor
        self.onBack = onBack
        self.onClose = onClose
        self.closeText = closeText
        self.rightActionContents = rightActionContents
    }

    var body: some View {
        HStack(alignment: .center, spacing: 16) {
            if let onBack {
                ActionButton(kind: .back, circleColor: buttonColor, iconColor: buttonTextColor, action: { onBack() })
            }
            if let title {
                Text(title)
                    .font(Typography.title2Bold)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityHeading(.h1)
                    .foregroundColor(titleColor)
            }
            Spacer()
            rightActionContents()
            if let onClose {
                if let closeText {
                    NavTextButton(string: closeText, backgroundColor: buttonColor, textColor: buttonTextColor) {
                        onClose()
                    }
                } else {
                    ActionButton(
                        kind: .close,
                        circleColor: buttonColor,
                        iconColor: buttonTextColor,
                        action: { onClose() }
                    )
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }
}

struct SheetHeader_Previews: PreviewProvider {
    static var previews: some View {
        List {
            SheetHeader(title: "This is a very long sheet title it should wrap", onClose: { print("Pressed") })
            SheetHeader(title: "short", onBack: { print("Pressed") })
            SheetHeader(title: "no back button")
        }
    }
}

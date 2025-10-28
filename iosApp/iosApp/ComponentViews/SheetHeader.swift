//
//  SheetHeader.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/10/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

struct SheetHeader<Title: View, RightActionContents: View>: View {
    let title: () -> Title
    let buttonColor: Color
    let buttonTextColor: Color
    let navCallbacks: NavigationCallbacks
    let closeText: String?
    let rightActionContents: () -> RightActionContents

    init(
        @ViewBuilder title: @escaping () -> Title,
        buttonColor: Color = Color.contrast,
        buttonTextColor: Color = Color.fill2,
        navCallbacks: NavigationCallbacks,
        closeText: String? = nil,
        @ViewBuilder rightActionContents: @escaping () -> RightActionContents = { EmptyView() }
    ) {
        self.title = title
        self.buttonColor = buttonColor
        self.buttonTextColor = buttonTextColor
        self.navCallbacks = navCallbacks
        self.closeText = closeText
        self.rightActionContents = rightActionContents
    }

    var body: some View {
        if navCallbacks.onBack != nil, navCallbacks.sheetBackState != .hidden, navCallbacks.onClose != nil {
            VStack(alignment: .leading) {
                HStack {
                    backButton
                    Spacer()
                    rightActionContents()
                    closeButton
                }
                HStack {
                    titleView
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.top, 16)
        } else {
            HStack(alignment: .center, spacing: 16) {
                backButton
                titleView
                Spacer()
                rightActionContents()
                closeButton
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.top, 16)
        }
    }

    @ViewBuilder private var backButton: some View {
        if let onBack = navCallbacks.onBack, navCallbacks.sheetBackState != .hidden {
            ActionButton(kind: .back, circleColor: buttonColor, iconColor: buttonTextColor, action: { onBack() })
                .preventScrollTaps()
        }
    }

    @ViewBuilder private var titleView: some View {
        title()
    }

    @ViewBuilder private var closeButton: some View {
        if let onClose = navCallbacks.onClose {
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
                ).preventScrollTaps()
            }
        }
    }
}

struct SheetHeaderTitle: View {
    let title: String
    let titleAccessibilityLabel: String?
    let titleColor: Color

    var body: some View {
        Text(title)
            .font(Typography.title2Bold)
            .accessibilityLabel(titleAccessibilityLabel ?? title)
            .accessibilityAddTraits(.isHeader)
            .accessibilityHeading(.h1)
            .foregroundColor(titleColor)
    }
}

extension SheetHeader where Title == SheetHeaderTitle {
    init(
        title: String,
        titleAccessibilityLabel: String? = nil,
        titleColor: Color = Color.text,
        buttonColor: Color = Color.contrast,
        buttonTextColor: Color = Color.fill2,
        navCallbacks: NavigationCallbacks,
        closeText: String? = nil,
        @ViewBuilder rightActionContents: @escaping () -> RightActionContents = { EmptyView() }
    ) {
        self.title = {
            SheetHeaderTitle(title: title, titleAccessibilityLabel: titleAccessibilityLabel, titleColor: titleColor)
        }
        self.buttonColor = buttonColor
        self.buttonTextColor = buttonTextColor
        self.navCallbacks = navCallbacks
        self.closeText = closeText
        self.rightActionContents = rightActionContents
    }
}

struct SheetHeader_Previews: PreviewProvider {
    static var previews: some View {
        List {
            SheetHeader(
                title: "This is a very long sheet title it should wrap",
                navCallbacks: .init(onBack: {}, onClose: {}, sheetBackState: .hidden)
            )
            SheetHeader(title: "short", navCallbacks: .init(onBack: {}, onClose: nil, sheetBackState: .shown))
            SheetHeader(
                title: "no back button",
                navCallbacks: .init(onBack: nil, onClose: nil, sheetBackState: .hidden)
            )
            SheetHeader(title: "Back and close", navCallbacks: .init(onBack: {}, onClose: {}, sheetBackState: .shown))
        }
    }
}

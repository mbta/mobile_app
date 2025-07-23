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
    let onBack: (() -> Void)?
    let onClose: (() -> Void)?
    let closeText: String?
    let rightActionContents: () -> Content

    init(
        title: String?,
        onBack: (() -> Void)? = nil,
        onClose: (() -> Void)? = nil,
        closeText: String? = nil,
        @ViewBuilder rightActionContents: @escaping () -> Content = { EmptyView() }
    ) {
        self.title = title
        self.onBack = onBack
        self.onClose = onClose
        self.closeText = closeText
        self.rightActionContents = rightActionContents
    }

    var body: some View {
        HStack(alignment: .center, spacing: 16) {
            if let onBack {
                ActionButton(kind: .back, action: { onBack() })
            }
            if let title {
                Text(title)
                    .font(Typography.title3Semibold)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityHeading(.h1)
            }
            Spacer()
            rightActionContents()
            if let onClose {
                if let closeText {
                    NavTextButton(string: closeText, backgroundColor: Color.key, textColor: Color.fill3) {
                        onClose()
                    }
                } else {
                    ActionButton(kind: .close, action: { onClose() })
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

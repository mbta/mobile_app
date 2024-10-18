//
//  SheetHeader.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/10/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct SheetHeader: View {
    var title: String?
    var onBack: (() -> Void)?
    var onClose: (() -> Void)?

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
            if let onClose {
                Spacer()
                ActionButton(kind: .close, action: { onClose() })
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

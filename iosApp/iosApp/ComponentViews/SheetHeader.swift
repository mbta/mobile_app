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
    var onClose: (() -> Void)?
    var title: String?

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            if let onClose {
                BackButton(action: { onClose() })
            }
            if let title {
                Text(title)
                    .font(Typography.title3Semibold)
                    .padding([.top], 1)
                    .accessibilityHeading(.h1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
    }
}

struct SheetHeader_Previews: PreviewProvider {
    static var previews: some View {
        List {
            SheetHeader(onClose: { print("Pressed") }, title: "This is a very long sheet title it should wrap")
            SheetHeader(onClose: { print("Pressed") }, title: "short")
            SheetHeader(title: "no back button")
        }
    }
}

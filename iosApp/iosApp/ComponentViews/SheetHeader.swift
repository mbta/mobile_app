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
    var onBackPress: (() -> Void)?
    var title: String?

    var body: some View {
        HStack(alignment: .top) {
            if let onBackPress {
                BackButton(onPress: { onBackPress() })
            }
            if let title {
                Text(title)
                    .font(.title2)
                    .fontWeight(.semibold)
                    .accessibilityHeading(.h1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
    }
}

struct SheetHeaer_Previews: PreviewProvider {
    static var previews: some View {
        List {
            SheetHeader(onBackPress: { print("Pressed") }, title: "This is a very long sheet title it should wrap")
            SheetHeader(onBackPress: { print("Pressed") }, title: "short")
            SheetHeader(title: "no back button")
        }
    }
}

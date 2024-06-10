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
    var onBackPress: (() -> Void)? = nil
    var title: String? = nil

    var body: some View {
        HStack {
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

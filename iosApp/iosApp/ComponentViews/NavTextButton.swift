//
//  NavTextButton.swift
//  iosApp
//
//  Created by Kayla Brady on 7/10/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct NavTextButton: View {
    let string: String
    let backgroundColor: Color
    let textColor: Color
    var height: CGFloat?
    var width: CGFloat?
    let action: () -> Void

    var body: some View {
        Button(
            action: action,
            label: {
                Text(string)
                    .font(Typography.callout)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .foregroundColor(textColor)
                    .frame(minWidth: width, minHeight: height)
                    .background(backgroundColor)
                    .buttonBorderShape(.capsule)
                    .clipShape(Capsule())
            }
        )
    }
}

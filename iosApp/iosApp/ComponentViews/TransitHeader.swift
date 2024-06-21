//
//  TransitHeader.swift
//  iosApp
//
//  Created by Simon, Emma on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct TransitHeader<Content: View>: View {
    let name: String
    let backgroundColor: Color
    let textColor: Color
    let modeIcon: Image
    var rightContent: () -> Content?

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    var body: some View {
        Label {
            Text(name)
                .font(.body).bold()
                .accessibilityHeading(.h2)
                .multilineTextAlignment(.leading)
                .foregroundStyle(textColor)
                .textCase(.none)
                .frame(maxWidth: .infinity, maxHeight: modeIconHeight, alignment: .leading)
            rightContent()
        } icon: {
            modeIcon
                .resizable()
                .aspectRatio(contentMode: .fit)
                .scaledToFit()
                .frame(maxHeight: modeIconHeight, alignment: .topLeading)
                .foregroundStyle(textColor)
        }
        .padding(8)
        .background(backgroundColor)
    }
}

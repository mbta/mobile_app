//
//  StopIconDot.swift
//  iosApp
//
//  Created by Tony Lizarraga on 10/16/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import SwiftUI

struct StopIconDot: View {
    @ObserveInjection var inject
    let iconName: String
    let targeted: Bool
    @ScaledMetric private var pinSize: CGFloat = 24
    @ScaledMetric private var imageSize: CGFloat = 20

    var body: some View {
        // Change this to get the icon it should
        Image(iconName)
            .resizable()
            .scaledToFill()
            .foregroundStyle(Color.deemphasized)
            // TODO: Should this have accessibility hidden?
            .frame(width: imageSize, height: imageSize).accessibilityHidden(true)
            .overlay {
                if targeted {
                    Image(.stopPinIndicator).resizable()
                        .aspectRatio(contentMode: .fit)
                        .scaledToFit()
                        .frame(width: pinSize, height: pinSize).padding(.bottom, pinSize).accessibilityHidden(true)
                }
            }
            .enableInjection()
    }
}

//
//  RecenterButton.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct RecenterButton: View {
    var icon: ImageResource
    var size: CGFloat
    var perform: () -> Void
    var body: some View {
        Image(icon)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .frame(width: 48, height: 48)
            .foregroundColor(.key)
            .background(Color(.fill3))
            .clipShape(Circle())
            .overlay(Circle().stroke(Color.halo, lineWidth: 2).frame(width: 50, height: 50))
            .padding(.horizontal, 20)
            .onTapGesture(perform: perform)
            .accessibilityLabel(Text(
                "Recenter map on my location",
                comment: "Screen reader text describing the behavior of the map recenter button"
            ))
            .accessibilityRemoveTraits(.isImage)
            .accessibilityAddTraits(.isButton)
            .accessibilityIdentifier("mapRecenterButton")
    }
}

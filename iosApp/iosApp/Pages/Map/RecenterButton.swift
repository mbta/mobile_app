//
//  RecenterButton.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct RecenterButton: View {
    var perform: () -> Void
    var body: some View {
        Image(.faLocationArrowSolid)
            .resizable()
            .scaledToFit()
            .frame(width: 17.33)
            .frame(width: 48, height: 48)
            .foregroundColor(.key)
            .background(Color(.fill3))
            .clipShape(Circle())
            .overlay(Circle().stroke(Color.halo, lineWidth: 2).frame(width: 50, height: 50))
            .padding(20)
            .onTapGesture(perform: perform)
            .transition(AnyTransition.opacity.animation(.linear(duration: 0.25)))
            .accessibilityIdentifier("mapRecenterButton")
    }
}

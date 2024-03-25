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
        Image(systemName: "location")
            .frame(width: 50, height: 50)
            .foregroundColor(.white)
            .background(.gray.opacity(0.8))
            .clipShape(Circle())
            .padding(20)
            .onTapGesture(perform: perform)
            .transition(AnyTransition.opacity.animation(.linear(duration: 0.25)))
            .accessibilityIdentifier("mapRecenterButton")
    }
}

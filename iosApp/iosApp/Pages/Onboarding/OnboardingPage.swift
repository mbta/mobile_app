//
//  OnboardingPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-10-25.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct OnboardingPage: View {
    let screens: [OnboardingScreen]
    @State var selectedIndex: Int = 0
    let onFinish: () -> Void

    let inspection = Inspection<Self>()

    var body: some View {
        OnboardingScreenView(screen: screens[selectedIndex], advance: {
            if selectedIndex < screens.count - 1 {
                selectedIndex += 1
            } else {
                onFinish()
            }
        })
        .padding(16)
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }
}

#Preview {
    OnboardingPage(screens: [.location, .hideMaps, .feedback], onFinish: {})
}

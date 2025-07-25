//
//  OnboardingPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-10-25.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import Shared
import SwiftUI

struct OnboardingPage: View {
    let screens: [OnboardingScreen]
    @State var selectedIndex: Int = 0

    let onFinish: () -> Void

    let onAdvance: () -> Void

    let onboardingRepository: IOnboardingRepository

    let skipLocationDialogue: Bool
    let inspection = Inspection<Self>()

    init(
        screens: [OnboardingScreen],
        onFinish: @escaping () -> Void,
        onAdvance: @escaping () -> Void = {},
        onboardingRepository: IOnboardingRepository = RepositoryDI().onboarding,
        skipLocationDialogue: Bool = false

    ) {
        self.screens = screens
        self.onFinish = onFinish
        self.onAdvance = onAdvance
        self.onboardingRepository = onboardingRepository
        self.skipLocationDialogue = skipLocationDialogue
    }

    var body: some View {
        let screen = screens[selectedIndex]
        OnboardingScreenView(screen: screen, advance: {
            Task {
                try? await onboardingRepository.markOnboardingCompleted(screen: screen)
                if selectedIndex < screens.count - 1 {
                    selectedIndex += 1
                    onAdvance()
                } else {
                    onFinish()
                }
            }
        }, skipLocationDialogue: skipLocationDialogue)
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }
}

#Preview {
    OnboardingPage(screens: OnboardingScreen.allCases, onFinish: {}, onboardingRepository: MockOnboardingRepository())
        .withFixedSettings([:])
}

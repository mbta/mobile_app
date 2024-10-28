//
//  OnboardingScreenView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-10-25.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import shared
import SwiftUI

struct OnboardingScreenView: View {
    let screen: OnboardingScreen
    let advance: () -> Void

    let createLocationFetcher: () -> any LocationFetcher
    let onboardingRepository: IOnboardingRepository
    let settingUseCase: SettingUsecase
    @State private var locationFetcher: LocationFetcher?
    private let locationPermissionHandler: LocationPermissionHandler

    let inspection = Inspection<Self>()

    init(
        screen: OnboardingScreen,
        advance: @escaping () -> Void,
        createLocationFetcher: @escaping () -> any LocationFetcher = { CLLocationManager() },
        onboardingRepository: IOnboardingRepository = RepositoryDI().onboarding,
        settingUseCase: SettingUsecase = UsecaseDI().settingUsecase
    ) {
        self.screen = screen
        self.advance = advance
        self.createLocationFetcher = createLocationFetcher
        self.onboardingRepository = onboardingRepository
        self.settingUseCase = settingUseCase
        locationPermissionHandler = LocationPermissionHandler(
            screen: screen,
            onboardingRepository: onboardingRepository,
            advance: advance
        )
    }

    var body: some View {
        VStack {
            switch screen {
            case .feedback:
                Text(
                    "MBTA Go is just getting started! We’re actively making improvements based on feedback from riders like you."
                )
                Button("Get started") {
                    Task {
                        try? await onboardingRepository.markOnboardingCompleted(screen: screen)
                        advance()
                    }
                }
            case .hideMaps:
                Text("For VoiceOver users, we’ll keep maps hidden by default unless you tell us otherwise.")
                Button("Hide maps") {
                    Task {
                        try await settingUseCase.set(setting: .hideMaps, value: true)
                        try? await onboardingRepository.markOnboardingCompleted(screen: screen)
                        advance()
                    }
                }
                Button("Show maps") {
                    Task {
                        try await settingUseCase.set(setting: .hideMaps, value: false)
                        try? await onboardingRepository.markOnboardingCompleted(screen: screen)
                        advance()
                    }
                }
            case .location:
                Text("We’ll use your location to show the lines and bus routes near you.")
                Button("Share location") {
                    locationFetcher = createLocationFetcher()

                    locationFetcher?.locationFetcherDelegate = locationPermissionHandler
                }
                Button("Not now") {
                    Task {
                        try? await onboardingRepository.markOnboardingCompleted(screen: screen)
                        advance()
                    }
                }
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    private class LocationPermissionHandler: NSObject, LocationFetcherDelegate, CLLocationManagerDelegate {
        let screen: OnboardingScreen
        let onboardingRepository: IOnboardingRepository
        let advance: () -> Void

        init(screen: OnboardingScreen, onboardingRepository: IOnboardingRepository, advance: @escaping () -> Void) {
            self.screen = screen
            self.onboardingRepository = onboardingRepository
            self.advance = advance
        }

        func locationFetcherDidChangeAuthorization(_ fetcher: LocationFetcher) {
            switch fetcher.authorizationStatus {
            case .notDetermined:
                fetcher.requestWhenInUseAuthorization()
            default:
                Task {
                    try? await onboardingRepository.markOnboardingCompleted(screen: screen)
                    advance()
                }
            }
        }

        func locationFetcher(_: any LocationFetcher, didUpdateLocations _: [CLLocation]) {}

        func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
            locationFetcherDidChangeAuthorization(manager)
        }
    }
}

#Preview {
    OnboardingScreenView(screen: .location, advance: {})
}

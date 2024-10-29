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
    let settingsRepository: ISettingsRepository
    @State private var locationFetcher: LocationFetcher?
    private let locationPermissionHandler: LocationPermissionHandler

    let inspection = Inspection<Self>()

    init(
        screen: OnboardingScreen,
        advance: @escaping () -> Void,
        createLocationFetcher: @escaping () -> any LocationFetcher = { CLLocationManager() },
        settingsRepository: ISettingsRepository = RepositoryDI().settings
    ) {
        self.screen = screen
        self.advance = advance
        self.createLocationFetcher = createLocationFetcher
        self.settingsRepository = settingsRepository
        locationPermissionHandler = LocationPermissionHandler(
            screen: screen,
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
                    advance()
                }
            case .hideMaps:
                Text("For VoiceOver users, we’ll keep maps hidden by default unless you tell us otherwise.")
                Button("Hide maps") {
                    Task {
                        try await settingsRepository.setSettings(settings: [.hideMaps: KotlinBoolean(bool: true)])
                        advance()
                    }
                }
                Button("Show maps") {
                    Task {
                        try await settingsRepository.setSettings(settings: [.hideMaps: KotlinBoolean(bool: false)])
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
                    advance()
                }
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    private class LocationPermissionHandler: NSObject, LocationFetcherDelegate, CLLocationManagerDelegate {
        let screen: OnboardingScreen
        let advance: () -> Void

        init(screen: OnboardingScreen, advance: @escaping () -> Void) {
            self.screen = screen
            self.advance = advance
        }

        func locationFetcherDidChangeAuthorization(_ fetcher: LocationFetcher) {
            switch fetcher.authorizationStatus {
            case .notDetermined:
                fetcher.requestWhenInUseAuthorization()
            default:
                advance()
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

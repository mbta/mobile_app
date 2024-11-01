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
    @State private var locationPermissionHandler: LocationPermissionHandler?

    @AccessibilityFocusState private var focusHeader: OnboardingScreen?
    @Environment(\.dynamicTypeSize) var typeSize
    @State private var pulse: CGFloat = 1

    private var screenHeight: CGFloat { UIScreen.current?.bounds.height ?? 852.0 }
    private var screenWidth: CGFloat { UIScreen.current?.bounds.width ?? 393.0 }

    // Use less padding on smaller screens
    private var bottomPadding: CGFloat { screenHeight < 812 ? 16 : 52 }
    private var sidePadding: CGFloat { screenWidth < 393 ? 16 : 32 }

    private var locationHaloSize: CGFloat { screenWidth * 0.8 }
    private var moreHaloSize: CGFloat { screenWidth * 0.55 }

    private var haloOffset: CGFloat {
        let height = screenHeight
        let width = screenWidth
        // If the aspect ratio of the screen is greater than the image,
        // it will be cut off on the top and bottom, which changes the
        // position of the icon we want the halo to be centered on
        // (390x844 are the background image dimensions)
        if width / height >= 390 / 844 {
            let scaledHeight = (width / 390) * 844
            return -((scaledHeight / 2) - (scaledHeight / 3) - 6)
        } else {
            return -((height / 2) - (height / 3))
        }
    }

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
        focusHeader = screen
    }

    var body: some View {
        VStack(spacing: 0) {
            switch screen {
            case .feedback:
                VStack(alignment: .leading, spacing: 16) {
                    Spacer()
                    Text("Help us improve")
                        .font(Typography.title1Bold)
                        .accessibilityHeading(.h1)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityFocused($focusHeader, equals: .feedback)
                    Text(
                        "MBTA Go is in the early stages! We want your feedback as we continue making improvements and adding new features."
                    )
                    .font(Typography.title3)
                    .padding(.bottom, 16)
                    .accessibilityHint(Text("Use the \"More\" navigation tab to send app feedback"))
                    .dynamicTypeSize(...DynamicTypeSize.accessibility3)
                    if typeSize >= .accessibility2, typeSize < .accessibility5 {
                        Spacer()
                    }
                    Button(action: advance) {
                        Text("Get started").onboardingKeyButton()
                    }
                }
                .padding(.horizontal, sidePadding)
                .padding(.bottom, bottomPadding)
                .background {
                    ZStack(alignment: .center) {
                        Color.fill2.edgesIgnoringSafeArea(.all)
                        if typeSize < .accessibility2 {
                            Image(.onboardingMoreButton)
                                .resizable()
                                .scaledToFill()
                                .edgesIgnoringSafeArea(.all)
                                .accessibilityHidden(true)
                            Image(.onboardingHalo)
                                .resizable()
                                .frame(width: moreHaloSize, height: moreHaloSize)
                                .scaleEffect(pulse)
                                .offset(x: 0, y: haloOffset)
                                .onAppear {
                                    pulse = 1
                                    withAnimation(.easeInOut(duration: 1.25).repeatForever(autoreverses: true)) {
                                        pulse = 1.22 * pulse
                                    }
                                }
                                .accessibilityHidden(true)
                        }
                    }
                }

            case .hideMaps:
                VStack(alignment: .leading, spacing: 16) {
                    Spacer()
                    VStack(alignment: .leading, spacing: 16) {
                        Text(
                            "Set map preference",
                            comment: "Onboarding screen header for asking VoiceOver users if they want to hide maps"
                        )
                        .font(Typography.title1Bold)
                        .accessibilityHeading(.h1)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityFocused($focusHeader, equals: .hideMaps)
                        Text(
                            "When using VoiceOver, we can skip reading out maps to keep you focused on transit information."
                        )
                        .font(Typography.title3)
                    }
                    .padding(32)
                    .background(Color.fill2)
                    .clipShape(.rect(cornerRadius: 32.0))
                    .shadow(radius: 16)
                    .dynamicTypeSize(...DynamicTypeSize.accessibility2)
                    Spacer()
                    Button(action: { hideMaps(true) }) {
                        Text("Hide maps", comment: "Onboarding button text for setting maps to hidden")
                            .onboardingKeyButton()
                    }
                    Button(action: { hideMaps(false) }) {
                        Text("Show maps", comment: "Onboarding button text for setting maps to shown")
                            .onboardingSecondaryButton()
                    }
                }
                .padding(.horizontal, sidePadding)
                .padding(.bottom, bottomPadding)
                .background {
                    Image(.onboardingBackgroundMap)
                        .resizable()
                        .scaledToFill()
                        .edgesIgnoringSafeArea(.all)
                        .accessibilityHidden(true)
                }

            case .location:
                VStack(alignment: .leading, spacing: 16) {
                    Spacer()
                    Text("See transit near you")
                        .font(Typography.title1Bold)
                        .accessibilityHeading(.h1)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityFocused($focusHeader, equals: .location)
                    Text("We use your location to show you nearby transit options.")
                        .font(Typography.title3)
                        .padding(.bottom, 16)
                    if typeSize >= .xxxLarge, typeSize < .accessibility3 {
                        Spacer()
                    }
                    Button(action: { shareLocation(true) }) {
                        Text("Allow Location Services").onboardingKeyButton()
                    }
                    Button(action: { shareLocation(false) }) {
                        Text(
                            "Skip for now",
                            comment: "Button text for deferring the request for location services"
                        ).onboardingSecondaryButton()
                    }
                }
                .dynamicTypeSize(...DynamicTypeSize.accessibility4)
                .padding(.horizontal, sidePadding)
                .padding(.bottom, bottomPadding)
                .background {
                    ZStack(alignment: .center) {
                        Image(.onboardingBackgroundMap)
                            .resizable()
                            .scaledToFill()
                            .accessibilityHidden(true)
                        if typeSize < .xxxLarge {
                            Image(.onboardingHalo)
                                .resizable()
                                .frame(width: locationHaloSize, height: locationHaloSize)
                                .scaleEffect(pulse)
                                .offset(x: 0, y: haloOffset)
                                .onAppear {
                                    pulse = 1
                                    withAnimation(.easeInOut(duration: 1.25).repeatForever(autoreverses: true)) {
                                        pulse = 1.15 * pulse
                                    }
                                }
                                .accessibilityHidden(true)
                            Image(.onboardingTransitLines)
                                .resizable()
                                .scaledToFill()
                                .accessibilityHidden(true)
                        }
                    }
                }.edgesIgnoringSafeArea(.all)
            }
        }
        .onChange(of: screen) { nextScreen in
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                // Voiceover won't pick this up if it's not delayed slightly
                focusHeader = nextScreen
            }
        }
        .onAppear {
            locationPermissionHandler = LocationPermissionHandler(
                screen: screen,
                advance: advance
            )
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    func hideMaps(_ hide: Bool) {
        Task {
            try await settingsRepository.setSettings(settings: [.hideMaps: KotlinBoolean(bool: hide)])
            advance()
        }
    }

    func shareLocation(_ share: Bool) {
        Task {
            try? await settingsRepository.setSettings(settings: [.locationDeferred: .init(bool: !share)])
        }
        if share {
            guard let locationPermissionHandler else { return }
            locationFetcher = createLocationFetcher()
            locationFetcher?.locationFetcherDelegate = locationPermissionHandler
        } else {
            advance()
        }
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

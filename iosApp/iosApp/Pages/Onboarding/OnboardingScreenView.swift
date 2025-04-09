//
//  OnboardingScreenView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-10-25.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import Shared
import SwiftUI

struct OnboardingScreenView: View {
    let screen: OnboardingScreen
    let advance: () -> Void

    let createLocationFetcher: () -> any LocationFetcher
    let skipLocationDialogue: Bool
    let settingsRepository: ISettingsRepository
    @State private var locationFetcher: LocationFetcher?
    @State private var locationPermissionHandler: LocationPermissionHandler?
    @State private var settings: [Settings: Bool] = [:]

    @AccessibilityFocusState private var focusHeader: OnboardingScreen?
    @Environment(\.dynamicTypeSize) var typeSize

    private var screenHeight: CGFloat { UIScreen.current?.bounds.height ?? 852.0 }
    private var screenWidth: CGFloat { UIScreen.current?.bounds.width ?? 393.0 }

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
        skipLocationDialogue: Bool = false,
        settingsRepository: ISettingsRepository = RepositoryDI().settings
    ) {
        self.screen = screen
        self.advance = advance
        self.createLocationFetcher = createLocationFetcher
        self.skipLocationDialogue = skipLocationDialogue
        self.settingsRepository = settingsRepository
        focusHeader = screen
    }

    var body: some View {
        VStack(spacing: 0) {
            switch screen {
            case .feedback:
                OnboardingPieces.PageColumn(content: {
                    Spacer()
                    OnboardingPieces.PageDescription(
                        headerText: Text("Help us improve"),
                        bodyText: Text(
                            "MBTA Go is in the early stages! We want your feedback as we continue making improvements and adding new features."
                        ),
                        focusBinding: $focusHeader,
                        focusValue: .feedback,
                        bodyAccessibilityHint: Text("Use the \"More\" navigation tab to send app feedback"),
                        bodyDynamicTypeSize: .accessibility3
                    )
                    .padding(.bottom, 16)
                    if typeSize >= .accessibility2, typeSize < .accessibility5 {
                        Spacer()
                    }
                    OnboardingPieces.KeyButton(text: Text("Get started"), action: advance)
                }, background: {
                    Color.fill2.edgesIgnoringSafeArea(.all)
                    if typeSize < .accessibility1 {
                        OnboardingPieces.BackgroundImage(.onboardingMoreButton)
                        OnboardingPieces.Halo(size: moreHaloSize, offsetY: haloOffset, pulseSize: 1.22)
                    }
                })

            case .hideMaps:
                OnboardingPieces.PageColumn(content: {
                    Spacer()
                    OnboardingPieces.PageDescription(
                        headerText: Text(
                            "Set map preference",
                            comment: "Onboarding screen header for asking VoiceOver users if they want to hide maps"
                        ),
                        bodyText: Text(
                            "When using VoiceOver, we can skip reading out maps to keep you focused on transit information."
                        ),
                        focusBinding: $focusHeader,
                        focusValue: .hideMaps
                    )
                    .padding(32)
                    .background(Color.fill2)
                    .clipShape(.rect(cornerRadius: 32.0))
                    .shadow(radius: 16)
                    .dynamicTypeSize(...DynamicTypeSize.accessibility2)
                    Spacer()
                    OnboardingPieces.KeyButton(
                        text: Text("Hide maps", comment: "Onboarding button text for setting maps to hidden"),
                        action: { hideMaps(true) }
                    )
                    OnboardingPieces.SecondaryButton(
                        text: Text("Show maps", comment: "Onboarding button text for setting maps to shown"),
                        action: { hideMaps(false) }
                    )
                }, background: {
                    OnboardingPieces.BackgroundImage(.onboardingBackgroundMap)
                })

            case .location:
                let illustrationCutoff: DynamicTypeSize = if screenHeight < 812 { .xxLarge } else { .xxxLarge }
                OnboardingPieces.PageColumn(content: {
                    Spacer()
                    OnboardingPieces.PageDescription(
                        headerText: Text("See transit near you"),
                        bodyText: Text("We use your location to show you nearby transit options."),
                        focusBinding: $focusHeader,
                        focusValue: .location
                    )
                    .padding(.bottom, 8)
                    if typeSize >= illustrationCutoff, typeSize < .accessibility3 {
                        Spacer()
                    }
                    OnboardingPieces.KeyButton(text: Text("Continue"), action: shareLocation)
                    Text("You can always change location settings later in the Settings app.")
                }, background: {
                    OnboardingPieces.BackgroundImage(.onboardingBackgroundMap)
                    if typeSize < illustrationCutoff {
                        OnboardingPieces.Halo(size: locationHaloSize, offsetY: haloOffset, pulseSize: 1.15)
                        OnboardingPieces.BackgroundImage(.onboardingTransitLines)
                    }
                })
                .dynamicTypeSize(...DynamicTypeSize.accessibility4)

            case .stationAccessibility:
                OnboardingPieces.PageColumn(content: {
                    Spacer()
                    if typeSize < .xxxLarge {
                        HStack {
                            Image(.accessibilityIconAccessibleLarge)
                                .resizable()
                                .frame(width: 196, height: 196)
                                .accessibilityHidden(true)
                        }.frame(maxWidth: .infinity, alignment: .center)
                    }
                    OnboardingPieces.PageDescription(
                        headerText: Text("Set station accessibility info preference"),
                        bodyText: Text(
                            "By opting in, we can show you which stations are inaccessible or have elevator closures."
                        ),
                        focusBinding: $focusHeader,
                        focusValue: .stationAccessibility
                    )
                    .padding(.bottom, 8)
                    OnboardingPieces.SettingsToggle(getSetting: { settings.getSafe(.stationAccessibility) },
                                                    toggleSetting: { toggleSetting(.stationAccessibility) },
                                                    label: Text("Station Accessibility Info"))
                    OnboardingPieces.KeyButton(text: Text(
                        "Continue",
                        comment: "Button to advance to next scren in onboarding flow"
                    ), action: { advance() })

                }, background: {
                    OnboardingPieces.BackgroundImage(.onboardingBackgroundMap)
                })
                .foregroundStyle(Color.text)
                .dynamicTypeSize(...DynamicTypeSize.accessibility4)
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

    func loadSettings() {
        Task {
            settings = try await settingsRepository.getSettings().mapValues { $0.boolValue }
        }
    }

    func hideMaps(_ hide: Bool) {
        Task {
            try await settingsRepository.setSettings(settings: [.hideMaps: KotlinBoolean(bool: hide)])
            advance()
        }
    }

    func shareLocation() {
        // short circuit for OnboardingPageView integration testing
        if skipLocationDialogue {
            advance()
        } else {
            guard let locationPermissionHandler else { return }
            locationFetcher = createLocationFetcher()
            locationFetcher?.locationFetcherDelegate = locationPermissionHandler
        }
    }

    func toggleSetting(_ setting: Settings) {
        Task {
            try await settingsRepository
                .setSetting(setting: setting, value: !settings.getSafe(setting))
            loadSettings()
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

#Preview("Feedback") {
    OnboardingScreenView(screen: .feedback, advance: {},
                         settingsRepository: MockSettingsRepository())
}

#Preview("Hide Maps") {
    OnboardingScreenView(screen: .hideMaps, advance: {},
                         settingsRepository: MockSettingsRepository())
}

#Preview("Location") {
    OnboardingScreenView(screen: .location, advance: {},
                         settingsRepository: MockSettingsRepository())
}

#Preview("Station Accessibility") {
    OnboardingScreenView(screen: .stationAccessibility, advance: {},
                         settingsRepository: MockSettingsRepository())
}

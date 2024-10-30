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
    let settingUseCase: SettingUsecase
    @State private var locationFetcher: LocationFetcher?

    private let locationPermissionHandler: LocationPermissionHandler

    @AccessibilityFocusState private var focusHeader: OnboardingScreen?
    @Environment(\.dynamicTypeSize) var typeSize
    @State private var pulse: CGFloat = 1

    private var haloOffset: CGFloat {
        let height = UIScreen.current?.bounds.height ?? 852.0
        return -((height / 2) - (height / 3) + 2)
    }

    private var locationHaloSize: CGFloat {
        let width = UIScreen.current?.bounds.width ?? 393.0
        return width * 0.8
    }

    private var moreHaloSize: CGFloat {
        let width = UIScreen.current?.bounds.width ?? 393.0
        return width * 0.55
    }

    let inspection = Inspection<Self>()

    init(
        screen: OnboardingScreen,
        advance: @escaping () -> Void,
        createLocationFetcher: @escaping () -> any LocationFetcher = { CLLocationManager() },
        settingUseCase: SettingUsecase = UsecaseDI().settingUsecase
    ) {
        self.screen = screen
        self.advance = advance
        self.createLocationFetcher = createLocationFetcher
        self.settingUseCase = settingUseCase
        locationPermissionHandler = LocationPermissionHandler(
            screen: screen,
            advance: advance
        )
        focusHeader = screen
    }

    var body: some View {
        VStack(spacing: 0) {
            switch screen {
            case .feedback:
                ZStack {
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
                                withAnimation(.easeInOut(duration: 5).repeatForever(autoreverses: true)) {
                                    pulse = 1.22 * pulse
                                }
                            }
                            .accessibilityHidden(true)
                    }
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
                    .padding(.horizontal, 32)
                    .padding(.bottom, 56)
                }
            case .hideMaps:
                ZStack {
                    Image(.onboardingBackgroundMap)
                        .resizable()
                        .scaledToFill()
                        .edgesIgnoringSafeArea(.all)
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 16) {
                        Spacer()
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Set map preference")
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
                        Button(action: { Task {
                            try await settingUseCase.set(setting: .hideMaps, value: true)
                            advance()
                        }}) {
                            Text("Hide maps").onboardingKeyButton()
                        }
                        Button(action: { Task {
                            try await settingUseCase.set(setting: .hideMaps, value: false)
                            advance()
                        }}) {
                            Text("Show maps").onboardingSecondaryButton()
                        }
                    }
                    .padding(.horizontal, 32)
                    .padding(.bottom, 56)
                }
            case .location:
                ZStack(alignment: .center) {
                    Image(.onboardingBackgroundMap)
                        .resizable()
                        .scaledToFill()
                        .edgesIgnoringSafeArea(.all)
                        .accessibilityHidden(true)
                    if typeSize < .xxxLarge {
                        Image(.onboardingHalo)
                            .resizable()
                            .frame(width: locationHaloSize, height: locationHaloSize)
                            .scaleEffect(pulse)
                            .offset(x: 0, y: haloOffset)
                            .onAppear {
                                pulse = 1
                                withAnimation(.easeInOut(duration: 5).repeatForever(autoreverses: true)) {
                                    pulse = 1.15 * pulse
                                }
                            }
                            .accessibilityHidden(true)
                        Image(.onboardingTransitLines)
                            .resizable()
                            .scaledToFill()
                            .edgesIgnoringSafeArea(.all)
                            .accessibilityHidden(true)
                    }
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
                        if typeSize >= .xxxLarge {
                            Spacer()
                        }
                        Button(action: {
                            locationFetcher = createLocationFetcher()
                            locationFetcher?.locationFetcherDelegate = locationPermissionHandler
                        }) {
                            Text("Allow Location Services").onboardingKeyButton()
                        }
                        Button(action: advance) {
                            Text("Skip for now").onboardingSecondaryButton()
                        }
                    }
                    .dynamicTypeSize(...DynamicTypeSize.accessibility4)
                    .padding(.horizontal, 32)
                    .padding(.bottom, 56)
                }
            }
        }
        .onChange(of: screen) { nextScreen in
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                // Voiceover won't pick this up if it's not delayed slightly
                focusHeader = nextScreen
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

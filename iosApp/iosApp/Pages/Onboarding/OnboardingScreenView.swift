//
//  OnboardingScreenView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-10-25.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import Lottie
import Shared
import SwiftUI

struct OnboardingScreenView: View {
    let screen: OnboardingScreen
    let advance: () -> Void

    let createLocationFetcher: () -> any LocationFetcher
    let skipLocationDialogue: Bool
    @EnvironmentObject var settingsCache: SettingsCache
    @State private var locationFetcher: LocationFetcher?
    @State private var locationPermissionHandler: LocationPermissionHandler?
    @State private var localHideMaps = true
    @State private var notificationsScreenState: NotificationsScreenState = .initial

    @AccessibilityFocusState private var focusHeader: OnboardingScreen?
    @Environment(\.colorScheme) var colorScheme
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
        skipLocationDialogue: Bool = false
    ) {
        self.screen = screen
        self.advance = advance
        self.createLocationFetcher = createLocationFetcher
        self.skipLocationDialogue = skipLocationDialogue
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
                        context: .onboarding,
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
                            "Set map display preference",
                            comment: "Onboarding screen header for asking VoiceOver users if they want to hide maps"
                        ),
                        bodyText: Text(
                            "When using VoiceOver, we can hide maps to make the app easier to navigate."
                        ),
                        focusBinding: $focusHeader,
                        focusValue: .hideMaps,
                        context: .onboarding,
                    )
                    .padding(32)
                    .background(Color.fill2)
                    .clipShape(.rect(cornerRadius: 32.0))
                    .shadow(radius: 16)
                    .dynamicTypeSize(...DynamicTypeSize.accessibility2)
                    Spacer()
                    OnboardingPieces.SettingsToggle(
                        getSetting: { !localHideMaps },
                        toggleSetting: { localHideMaps = !localHideMaps },
                        label: Text("Map Display")
                    )
                    OnboardingPieces.KeyButton(
                        text: Text("Continue"),
                        action: {
                            settingsCache.set(.hideMaps, localHideMaps)
                            advance()
                        }
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
                        focusValue: .location,
                        context: .onboarding,
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

            case .notificationsBeta:
                notificationsBeta

            case .stationAccessibility:
                OnboardingPieces.PageColumn(content: {
                    Spacer()
                    if typeSize < .xxxLarge {
                        HStack {
                            Image(.accessibilityIconAccessible)
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
                        focusValue: .stationAccessibility,
                        context: .onboarding,
                    )
                    .padding(.bottom, 8)
                    OnboardingPieces.SettingsToggle(
                        getSetting: { settingsCache.get(.stationAccessibility) },
                        toggleSetting: { settingsCache.set(
                            .stationAccessibility,
                            !settingsCache.get(.stationAccessibility)
                        ) },
                        label: Text("Station Accessibility Info")
                    )
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

    private enum NotificationsScreenState: Comparable {
        case initial
        case afterFavorite
        case afterSchedule
        case final
    }

    @ViewBuilder private var notificationsBeta: some View {
        VStack {
            HStack(spacing: 10) {
                Image(.appIcon).resizable().scaledToFit().frame(width: 38, height: 38)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: 0) {
                        Text(verbatim: "Orange Line")
                            .font(.system(size: 16, weight: .semibold))
                        Spacer()
                        Text("now")
                            .font(.system(size: 16))
                    }
                    let alert = FormattedAlert(alert: nil, alertSummary: .init(
                        effect: .suspension,
                        location: AlertSummary.LocationSuccessiveStops(
                            startStopName: "Back Bay",
                            endStopName: "Wellington"
                        ),
                        timeframe: AlertSummary.TimeframeThisWeek(time: .init(
                            year: 2026,
                            month: .january,
                            day: 25,
                            hour: 12,
                            minute: 0,
                            second: 0
                        )),
                        recurrence: nil
                    ))
                    Text(String(alert.alertCardMajorBody.characters[...]))
                        .font(.system(size: 16))
                        .frame(minHeight: 40) // TODO: fix
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(colorScheme == .dark ? Color.black.opacity(0.5) : Color.white.opacity(0.75))
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .padding(16)
            .padding(.top, 100)
            .padding(.bottom, 32)
        }
        .background(Color.key)
        .withRoundedBorder(radius: 24, color: .black, width: 12)
        .ignoresSafeArea()
        .scaleEffect(0.75, anchor: .top)
        .frame(height: 160, alignment: .bottom)
        OnboardingPieces.PageColumn(content: {
            VStack(alignment: .leading, spacing: 32) {
                let header = NSLocalizedString("Now get disruption notifications", comment: "")
                Text(header)
                    .font(Typography.title1Bold)
                    .accessibilityLabel(Text("New feature. \(header)"))
                    .accessibilityHeading(.h1)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityFocused($focusHeader, equals: .notificationsBeta)

                HStack(spacing: 12) {
                    StarIcon(starred: notificationsScreenState >= .afterFavorite, color: .key, size: 36).frame(
                        width: 52,
                        alignment: .center
                    )
                    Text("Add a Favorite stop")
                        .font(Typography.title3)
                        .opacity(notificationsScreenState >= .afterFavorite ? 1 : 0.4)
                        .animation(.default, value: notificationsScreenState)
                }

                HStack(spacing: 12) {
                    Toggle(isOn: .constant(notificationsScreenState >= .afterSchedule), label: {}).scaleEffect(
                        0.75,
                        anchor: .center
                    ).frame(width: 52, alignment: .center).tint(.key)
                    Text("Set your own schedule")
                        .font(Typography.title3)
                        .opacity(notificationsScreenState >= .afterSchedule ? 1 : 0.4)
                    Spacer()
                }

                Text("We’ll tell you about any problems before you go!")
                    .font(Typography.title3)
                    .opacity(notificationsScreenState >= .final ? 1 : 0.4)

                ZStack(alignment: .bottom) {
                    LottieView {
                        try await DotLottieFile
                            .named(colorScheme == .dark ? "Notification pop-dark" : "Notification pop-light")
                    }
                    .playbackMode(notificationsScreenState >= .final ? .playing(.toProgress(
                        1,
                        loopMode: .playOnce
                    )) : .paused(at: .time(0)))

                    OnboardingPieces.KeyButton(
                        text: Text(
                            "Got it",
                            comment: "The continue button text on a page displaying new features since last update"
                        ),
                        action: advance
                    )
                }
            }
        }, background: {})
            .foregroundStyle(Color.text)
            .frame(maxHeight: .infinity)
            .task {
                try? await Task.sleep(for: .seconds(2))
                notificationsScreenState = .afterFavorite
                try? await Task.sleep(for: .seconds(2))
                notificationsScreenState = .afterSchedule
                try? await Task.sleep(for: .seconds(2))
                notificationsScreenState = .final
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
    OnboardingScreenView(screen: .feedback, advance: {})
        .withFixedSettings([:])
}

#Preview("Map Display") {
    OnboardingScreenView(screen: .hideMaps, advance: {})
        .withFixedSettings([:])
}

#Preview("Location") {
    OnboardingScreenView(screen: .location, advance: {})
        .withFixedSettings([:])
}

#Preview("Notifications Beta") {
    OnboardingScreenView(screen: .notificationsBeta, advance: {})
        .withFixedSettings([:])
}

#Preview("Station Accessibility") {
    OnboardingScreenView(screen: .stationAccessibility, advance: {})
        .withFixedSettings([:])
}

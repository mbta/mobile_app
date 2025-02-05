//
//  PromoScreenView.swift
//  iosApp
//
//  Created by esimon on 2/5/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreLocation
import shared
import SwiftUI

struct PromoScreenView: View {
    let screen: FeaturePromo
    let advance: () -> Void

    @AccessibilityFocusState private var focusHeader: FeaturePromo?
    @Environment(\.dynamicTypeSize) var typeSize

    private var screenHeight: CGFloat { UIScreen.current?.bounds.height ?? 852.0 }
    private var screenWidth: CGFloat { UIScreen.current?.bounds.width ?? 393.0 }

    // Use less padding on smaller screens
    private var bottomPadding: CGFloat { screenHeight < 812 ? 16 : 52 }
    private var sidePadding: CGFloat { screenWidth < 393 ? 16 : 32 }

    let inspection = Inspection<Self>()

    init(screen: FeaturePromo, advance: @escaping () -> Void) {
        self.screen = screen
        self.advance = advance
        focusHeader = screen
    }

    var body: some View {
        VStack(spacing: 0) {
            switch screen {
            case .combinedStopAndTrip: combinedStopAndTrip
            default: EmptyView()
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

    @ViewBuilder
    var combinedStopAndTrip: some View {
        VStack(alignment: .leading, spacing: 16) {
            Spacer()
            Text(
                "See arrivals and track vehicles in one place",
                comment: """
                Promo text header that displays when users first open the app after a redesign of the stop page
                """
            )
            .font(Typography.title1Bold)
            .accessibilityHeading(.h1)
            .accessibilityAddTraits(.isHeader)
            .accessibilityFocused($focusHeader, equals: .combinedStopAndTrip)
            Text(
                "We created a new view that allows you to see arrivals at your stop and track vehicle locations all at once. Send us feedback to let us know what you think!",
                comment: "Promo text that displays when users first open the app after a redesign of the stop page"
            )
            .font(Typography.title3)
            .padding(.bottom, 16)
            .dynamicTypeSize(...DynamicTypeSize.accessibility3)
            if typeSize >= .accessibility2, typeSize < .accessibility5 {
                Spacer()
            }
            Button(action: advance) {
                Text(
                    "Got it",
                    comment: "The continue button text on a page displaying new features since last update"
                ).fullWidthKeyButton()
            }
        }
        .padding(.horizontal, sidePadding)
        .padding(.bottom, bottomPadding)
        .background {
            ZStack(alignment: .center) {
                Color.fill2.edgesIgnoringSafeArea(.all)
                if typeSize < .accessibility2 {
                    // TODO: Add finalized graphic
//                    Image(.promoCombinedStop)
//                        .resizable()
//                        .accessibilityHidden(true)
                }
            }
        }
    }
}

#Preview {
    PromoScreenView(screen: .combinedStopAndTrip, advance: {})
}

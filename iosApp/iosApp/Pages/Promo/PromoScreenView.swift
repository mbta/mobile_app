//
//  PromoScreenView.swift
//  iosApp
//
//  Created by esimon on 2/5/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreLocation
import Shared
import SwiftUI

struct PromoScreenView: View {
    let screen: FeaturePromo
    let advance: () -> Void

    @AccessibilityFocusState private var focusHeader: FeaturePromo?
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.dynamicTypeSize) var typeSize

    private var screenHeight: CGFloat { UIScreen.current?.bounds.height ?? 852.0 }
    private var screenWidth: CGFloat { UIScreen.current?.bounds.width ?? 393.0 }

    // Use less padding on smaller screens
    private var bottomPadding: CGFloat { screenHeight < 812 ? 16 : 52 }
    private var sidePadding: CGFloat { 32 }

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
        let promoDetailsKey = NSLocalizedString(
            "We now show **arrivals** and detailed **vehicle locations** all at once. Let us know what you think!",
            comment: "Promo text that displays when users first open the app after a redesign of the stop page"
        )

        var promoDetailsString: AttributedString {
            do {
                return try AttributedString(markdown: promoDetailsKey)
            } catch {
                return AttributedString(promoDetailsKey.filter { $0 != "*" })
            }
        }
        OnboardingPieces.PageColumn(content: {
            if typeSize < .accessibility2 {
                Image(.featPromoComboStopTrip)
                    .resizable()
                    .scaledToFill()
                    .accessibilityHidden(true)
            }
            OnboardingPieces.PageDescription(
                headerText: Text(
                    "Check out the new stop view",
                    comment: """
                    Promo text header that displays when users first open the app after a redesign of the stop page
                    """
                ),
                bodyText: Text(promoDetailsString),
                focusBinding: $focusHeader,
                focusValue: .combinedStopAndTrip,
                bodyDynamicTypeSize: .accessibility3
            )
            .padding(.bottom, 16)
            if typeSize >= .accessibility2, typeSize < .accessibility5 {
                Spacer()
            }

            OnboardingPieces.KeyButton(
                text: Text(
                    "Got it",
                    comment: "The continue button text on a page displaying new features since last update"
                ),
                action: advance
            )
        }, background: {
            colorScheme == .dark ? Color.fill1 : Color.fill2
        })
        .foregroundStyle(Color.text)
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    PromoScreenView(screen: .combinedStopAndTrip, advance: {})
}

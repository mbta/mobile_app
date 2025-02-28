//
//  OnboardingPieces.swift
//  iosApp
//
//  Created by Melody Horn on 2/27/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

enum OnboardingPieces {
    struct PageDescription<FocusValue: Hashable>: View {
        let headerText: Text
        let bodyText: Text

        let focusBinding: AccessibilityFocusState<FocusValue>.Binding
        let focusValue: FocusValue

        let bodyDynamicTypeSize: DynamicTypeSize?
        let bodyAccessibilityHint: Text?

        init(
            headerText: Text,
            bodyText: Text,
            focusBinding: AccessibilityFocusState<FocusValue>.Binding,
            focusValue: FocusValue,
            bodyAccessibilityHint: Text? = nil,
            bodyDynamicTypeSize: DynamicTypeSize? = nil
        ) {
            self.headerText = headerText
            self.bodyText = bodyText
            self.focusBinding = focusBinding
            self.focusValue = focusValue
            self.bodyAccessibilityHint = bodyAccessibilityHint
            self.bodyDynamicTypeSize = bodyDynamicTypeSize
        }

        var body: some View {
            VStack(alignment: .leading, spacing: 16) {
                headerText
                    .font(Typography.title1Bold)
                    .accessibilityHeading(.h1)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityFocused(focusBinding, equals: focusValue)
                sizeHintBodyText
            }
        }

        private var leafBodyText: some View {
            bodyText
                .font(Typography.title3)
                .padding(.bottom, 16)
        }

        @ViewBuilder private var hintBodyText: some View {
            if let bodyAccessibilityHint {
                leafBodyText.accessibilityHint(bodyAccessibilityHint)
            } else {
                leafBodyText
            }
        }

        @ViewBuilder private var sizeHintBodyText: some View {
            if let bodyDynamicTypeSize {
                hintBodyText.dynamicTypeSize(...bodyDynamicTypeSize)
            } else {
                hintBodyText
            }
        }
    }

    struct PageColumn<Content: View, Background: View>: View {
        @ViewBuilder let content: () -> Content
        @ViewBuilder let background: () -> Background

        private var screenHeight: CGFloat { UIScreen.current?.bounds.height ?? 852.0 }
        // Use less padding on smaller screens
        private var bottomPadding: CGFloat { screenHeight < 812 ? 16 : 28 }

        var body: some View {
            VStack(alignment: .leading, spacing: 16) {
                content()
            }
            .padding(.horizontal, 32)
            .padding(.bottom, bottomPadding)
            .background {
                ZStack(alignment: .center) {
                    background()
                }
                .ignoresSafeArea()
            }
        }
    }

    struct KeyButton: View {
        let text: Text
        let action: () -> Void

        var body: some View {
            Button(action: action) {
                text.fullWidthKeyButton()
            }
        }
    }

    struct SecondaryButton: View {
        let text: Text
        let action: () -> Void

        var body: some View {
            Button(action: action) {
                text.fullWidthSecondaryButton()
            }
        }
    }

    struct BackgroundImage: View {
        let image: ImageResource

        init(_ image: ImageResource) {
            self.image = image
        }

        var body: some View {
            Image(image)
                .resizable()
                .scaledToFill()
                .edgesIgnoringSafeArea(.all)
                .accessibilityHidden(true)
        }
    }

    struct Halo: View {
        let size: CGFloat
        let offsetY: CGFloat
        let pulseSize: Double

        @State private var pulse = 0.0

        var body: some View {
            Image(.onboardingHalo)
                .resizable()
                .frame(width: size, height: size)
                .scaleEffect(pulse)
                .offset(x: 0, y: offsetY)
                .onAppear {
                    pulse = 1
                    withAnimation(.easeInOut(duration: 1.25).repeatForever(autoreverses: true)) {
                        pulse = pulseSize * pulse
                    }
                }
                .accessibilityHidden(true)
            Image(.onboardingHalo)
                .resizable()
                .frame(width: size, height: size)
                .scaleEffect(pulse)
                .offset(x: 0, y: offsetY)
                .onAppear {
                    pulse = 1
                    withAnimation(.easeInOut(duration: 1.25).repeatForever(autoreverses: true)) {
                        pulse = 1.15 * pulse
                    }
                }
                .accessibilityHidden(true)
        }
    }
}

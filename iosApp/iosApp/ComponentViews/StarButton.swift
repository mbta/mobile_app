//
//  StarButton.swift
//  iosApp
//
//  Created by Simon, Emma on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StarButton: View {
    let starred: Bool
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(
            action: action,
            label: { StarIcon(starred: starred, color: color) }
        )
        .accessibilityIdentifier("starButton")
        .accessibilityAddTraits(starred ? [.isSelected] : [])
        .preventScrollTaps()
    }
}

private extension Animation {
    static func cssEaseIn(duration: TimeInterval) -> Self {
        timingCurve(0, 0, 0.58, 1, duration: duration)
    }

    static func cssEaseOut(duration: TimeInterval) -> Self {
        timingCurve(0.42, 0, 1, 1, duration: duration)
    }

    static var snap: Self {
        linear(duration: 0.001)
    }
}

struct StarIcon: View {
    let starred: Bool
    let color: Color

    @ScaledMetric private var size: CGFloat
    var strokeScale: Double {
        size / 20
    }

    init(starred: Bool, color: Color, size: CGFloat = 20) {
        self.starred = starred
        self.color = color
        _size = .init(wrappedValue: size)
    }

    struct OuterStarShape: SwiftUI.Shape {
        func path(in rect: CGRect) -> SwiftUI.Path {
            let outerShape = FavoriteStarShape(rect: rect.into(), scale: 1)
            return Path {
                $0.move(to: outerShape.arms.first!.in.into())
                for arm in outerShape.arms {
                    $0.addLine(to: arm.in.into())
                    $0.addLine(to: arm.outStart.into())
                    $0.addCurve(
                        to: arm.outMid.into(),
                        control1: arm.outStartControl.into(),
                        control2: arm.outMidStartControl.into()
                    )
                    $0.addCurve(
                        to: arm.outEnd.into(),
                        control1: arm.outMidEndControl.into(),
                        control2: arm.outEndControl.into()
                    )
                }
                $0.closeSubpath()
            }
        }
    }

    struct OuterInnerStarShape: SwiftUI.Shape {
        var starred: Bool
        var innerSize: Float

        var animatableData: Float {
            get { innerSize }
            set { innerSize = newValue }
        }

        func path(in rect: CGRect) -> SwiftUI.Path {
            let outerShape = FavoriteStarShape(rect: rect.into(), scale: 1)
            let innerShape = FavoriteStarShape(rect: rect.into(), scale: innerSize)
            return Path {
                $0.move(to: outerShape.arms.first!.in.into())
                for arm in outerShape.arms {
                    $0.addLine(to: arm.in.into())
                    $0.addLine(to: arm.outStart.into())
                    $0.addCurve(
                        to: arm.outMid.into(),
                        control1: arm.outStartControl.into(),
                        control2: arm.outMidStartControl.into()
                    )
                    $0.addCurve(
                        to: arm.outEnd.into(),
                        control1: arm.outMidEndControl.into(),
                        control2: arm.outEndControl.into()
                    )
                }
                $0.closeSubpath()
                if !starred, innerSize > 0 {
                    // unstarring, draw inner as hole
                    $0.move(to: innerShape.arms.last!.outEnd.into())
                    for arm in innerShape.arms.reversed() {
                        $0.addLine(to: arm.outEnd.into())
                        $0.addCurve(
                            to: arm.outMid.into(),
                            control1: arm.outEndControl.into(),
                            control2: arm.outMidEndControl.into()
                        )
                        $0.addCurve(
                            to: arm.outStart.into(),
                            control1: arm.outMidStartControl.into(),
                            control2: arm.outStartControl.into()
                        )
                        $0.addLine(to: arm.in.into())
                    }
                    $0.closeSubpath()
                }
            }
        }
    }

    struct InnerStarShape: SwiftUI.Shape {
        var innerSize: Float

        var animatableData: Float {
            get { innerSize }
            set { innerSize = newValue }
        }

        func path(in rect: CGRect) -> SwiftUI.Path {
            if innerSize == 0 { return .init() }
            let innerShape = FavoriteStarShape(rect: rect.into(), scale: innerSize)
            return Path {
                $0.move(to: innerShape.arms.first!.in.into())
                for arm in innerShape.arms {
                    $0.addLine(to: arm.in.into())
                    $0.addLine(to: arm.outStart.into())
                    $0.addCurve(
                        to: arm.outMid.into(),
                        control1: arm.outStartControl.into(),
                        control2: arm.outMidStartControl.into()
                    )
                    $0.addCurve(
                        to: arm.outEnd.into(),
                        control1: arm.outMidEndControl.into(),
                        control2: arm.outEndControl.into()
                    )
                }
                $0.closeSubpath()
            }
        }
    }

    @State var strokeColor: Color = .clear
    @State var outerFillColor: Color = .clear
    @State var innerSize: Float = 0
    @State var strokeWidth: Double = 1
    @State var strokeProportionBehindFill: Double = 1

    var body: some View {
        ZStack {
            OuterStarShape().stroke(
                strokeColor.opacity(strokeProportionBehindFill),
                style: .init(lineWidth: strokeWidth * strokeScale, lineJoin: .round)
            )
            OuterInnerStarShape(starred: starred, innerSize: innerSize).fill(outerFillColor)
            if starred {
                // starring, draw inner as separate path
                InnerStarShape(innerSize: innerSize).fill(color)
            }
            OuterStarShape().stroke(
                strokeColor.opacity(1 - strokeProportionBehindFill),
                style: .init(lineWidth: strokeWidth * strokeScale, lineJoin: .round)
            )
        }
        .onAppear { setStarred(starred) }
        .onChange(of: starred) { animateStarred($0) }
        .frame(width: size, height: size)
        .accessibilityLabel(Text(
            "Star route",
            comment: "VoiceOver label for the button to favorite a route"
        ))
        .accessibilityHint(starred ? NSLocalizedString(
            "Unpins route from the top of the list",
            comment: "VoiceOver hint for favorite button when a route is already favorited"
        ) : NSLocalizedString(
            "Pins route to the top of the list",
            comment: "VoiceOver hint for favorite button when a route is not favorited"
        ))
    }

    func setStarred(_ starred: Bool) {
        if starred {
            strokeColor = .deemphasized.opacity(0.1)
            outerFillColor = color
            strokeProportionBehindFill = 0
        } else {
            strokeColor = color
            outerFillColor = .clear
            strokeProportionBehindFill = 1
        }
        innerSize = 0
        strokeWidth = 1
    }

    func animateStarred(_ starred: Bool) {
        if starred {
            if #available(iOS 17.0, *) {
                withAnimation(.cssEaseOut(duration: 0.2)) {
                    strokeColor = color.opacity(0.33)
                    innerSize = 1
                    strokeWidth = 10
                } completion: {
                    outerFillColor = color
                    withAnimation(.cssEaseIn(duration: 0.2).delay(0.05)) {
                        strokeColor = .deemphasized.opacity(0.1)
                        strokeWidth = 1
                        strokeProportionBehindFill = 0
                    } completion: {
                        innerSize = 0
                    }
                }
            } else {
                withAnimation(.cssEaseOut(duration: 0.2)) {
                    strokeColor = color.opacity(0.33)
                    innerSize = 1
                    strokeWidth = 10
                }
                withAnimation(.snap.delay(0.25)) {
                    outerFillColor = color
                }
                withAnimation(.cssEaseIn(duration: 0.2).delay(0.25)) {
                    strokeColor = .deemphasized.opacity(0.1)
                    strokeWidth = 1
                    strokeProportionBehindFill = 0
                }
                Task {
                    try? await Task.sleep(for: .milliseconds(450))
                    innerSize = 0
                }
            }
        } else {
            withAnimation(.cssEaseIn(duration: 0.2)) {
                strokeColor = color
                innerSize = 1
                strokeWidth = 1
            }
            withAnimation(.snap.delay(0.2)) {
                outerFillColor = .clear
                innerSize = 0
                strokeProportionBehindFill = 1
            }
        }
    }
}

private struct PreviewHelper: View {
    @State var starred = false

    var body: some View {
        StarIcon(starred: starred, color: .init(hex: "FFC72C"), size: 120)
            .task {
                while true {
                    try? await Task.sleep(for: .seconds(1))
                    starred = !starred
                    try? await Task.sleep(for: .milliseconds(500))
                }
            }
    }
}

#Preview {
    PreviewHelper()
}

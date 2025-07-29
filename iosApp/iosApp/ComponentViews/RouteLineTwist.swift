//
//  RouteLineTwist.swift
//  iosApp
//
//  Created by Melody Horn on 7/24/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteLineTwist: View {
    let color: Color
    let proportionClosed: Double
    let connections: [(RouteBranchSegment.StickConnection, Bool)]

    // according to https://swiftui-lab.com/swiftui-animations-part1/ this is how you get animated paths in SwiftUI

    struct TwistShadowShape: SwiftUI.Shape {
        var proportionClosed: Double
        let connection: RouteBranchSegment.StickConnection

        var animatableData: Double {
            get { proportionClosed }
            set { proportionClosed = newValue }
        }

        func path(in rect: CGRect) -> Path {
            let openFromVPos = connection.fromVPos == .center ? .bottom : connection.fromVPos
            let openToVPos = connection.toVPos == .center ? .bottom : connection.toVPos
            let opensToNothing = openFromVPos == openToVPos
            if opensToNothing, proportionClosed == 0 { return Path() }
            let topY = RouteLineTwist.lerp(
                StickDiagram.y(openFromVPos, size: rect.size),
                StickDiagram.y(connection.fromVPos, size: rect.size),
                t: proportionClosed
            )
            let bottomY = RouteLineTwist.lerp(
                StickDiagram.y(openToVPos, size: rect.size),
                StickDiagram.y(connection.toVPos, size: rect.size),
                t: proportionClosed
            )
            let centerY = (topY + bottomY) / 2
            let topCenterX = StickDiagram.x(connection.fromLane, size: rect.size)
            // when the twist is untwisted, we want to keep using the from lane so that the segment below the toggle
            // lines up right
            let bottomCenterX = lerp(
                topCenterX,
                StickDiagram.x(connection.toLane, size: rect.size),
                t: proportionClosed
            )
            let height = bottomY - topY
            let twistSlantDY = height / 32 * proportionClosed
            let centerCenterX = (topCenterX + bottomCenterX) / 2
            let twistSlantDX = rect.width / 8 * proportionClosed
            return Path {
                $0.move(to: .init(x: centerCenterX + twistSlantDX, y: centerY + twistSlantDY))
                $0.addLine(to: .init(x: centerCenterX - twistSlantDX, y: centerY - twistSlantDY))
            }
        }
    }

    struct TwistCurvesShape: SwiftUI.Shape {
        var proportionClosed: Double
        let connection: RouteBranchSegment.StickConnection

        var animatableData: Double {
            get { proportionClosed }
            set { proportionClosed = newValue }
        }

        func path(in rect: CGRect) -> Path {
            let openFromVPos = connection.fromVPos == .center ? .bottom : connection.fromVPos
            let openToVPos = connection.toVPos == .center ? .bottom : connection.toVPos
            let opensToNothing = openFromVPos == openToVPos
            if opensToNothing, proportionClosed == 0 { return Path() }
            let topY = lerp(
                StickDiagram.y(openFromVPos, size: rect.size),
                StickDiagram.y(connection.fromVPos, size: rect.size),
                t: proportionClosed
            )
            let bottomY = lerp(
                StickDiagram.y(openToVPos, size: rect.size),
                StickDiagram.y(connection.toVPos, size: rect.size),
                t: proportionClosed
            )
            let centerY = (topY + bottomY) / 2
            let topCenterX = StickDiagram.x(connection.fromLane, size: rect.size)
            // when the twist is untwisted, we want to keep using the from lane so that the segment below the toggle
            // lines up right
            let bottomCenterX = lerp(
                topCenterX,
                StickDiagram.x(connection.toLane, size: rect.size),
                t: proportionClosed
            )
            let height = bottomY - topY
            let twistSlantDY = height / 32 * proportionClosed
            let nearTwistDY = height / 9 * proportionClosed
            let curveStartDY = height / 5
            let curveStartControlDY = rect.height / 13
            let curveEndControlDY = rect.height / 20
            let centerCenterX = (topCenterX + bottomCenterX) / 2
            let twistSlantDX = rect.width / 8 * proportionClosed
            let nearTwistDX = rect.width / 12 * proportionClosed
            let curveEndControlDX = rect.width / 15 * proportionClosed
            return Path {
                if bottomY != rect.maxY {
                    $0.move(to: .init(x: bottomCenterX, y: bottomY))
                    $0.addLine(to: .init(x: bottomCenterX, y: bottomY - curveStartDY))
                } else {
                    $0.move(to: .init(x: bottomCenterX, y: bottomY - curveStartDY))
                }
                $0.addCurve(
                    to: .init(x: centerCenterX + nearTwistDX, y: centerY + nearTwistDY),
                    control1: .init(x: bottomCenterX, y: bottomY - curveStartDY - curveStartControlDY),
                    control2: .init(
                        x: centerCenterX + nearTwistDX - curveEndControlDX,
                        y: centerY + nearTwistDY + curveEndControlDY
                    )
                )
                $0.addLine(to: .init(x: centerCenterX + twistSlantDX, y: centerY + twistSlantDY))
                $0.move(to: .init(x: centerCenterX - twistSlantDX, y: centerY - twistSlantDY))
                $0.addLine(to: .init(x: centerCenterX - nearTwistDX, y: centerY - nearTwistDY))
                $0.addCurve(
                    to: .init(x: topCenterX, y: topY + curveStartDY),
                    control1: .init(
                        x: centerCenterX - nearTwistDX + curveEndControlDX,
                        y: centerY - nearTwistDY - curveEndControlDY
                    ),
                    control2: .init(x: topCenterX, y: topY + curveStartDY + curveStartControlDY)
                )
                if topY != rect.minY {
                    $0.addLine(to: .init(x: topCenterX, y: topY))
                }
            }
        }
    }

    struct TwistEndsShape: SwiftUI.Shape {
        var proportionClosed: Double
        let connection: RouteBranchSegment.StickConnection

        var animatableData: Double {
            get { proportionClosed }
            set { proportionClosed = newValue }
        }

        func path(in rect: CGRect) -> Path {
            let openFromVPos = connection.fromVPos == .center ? .bottom : connection.fromVPos
            let openToVPos = connection.toVPos == .center ? .bottom : connection.toVPos
            let opensToNothing = openFromVPos == openToVPos
            if opensToNothing, proportionClosed == 0 { return Path() }
            let topY = lerp(
                StickDiagram.y(openFromVPos, size: rect.size),
                StickDiagram.y(connection.fromVPos, size: rect.size),
                t: proportionClosed
            )
            let bottomY = lerp(
                StickDiagram.y(openToVPos, size: rect.size),
                StickDiagram.y(connection.toVPos, size: rect.size),
                t: proportionClosed
            )
            let topCenterX = StickDiagram.x(connection.fromLane, size: rect.size)
            // when the twist is untwisted, we want to keep using the from lane so that the segment below the toggle
            // lines up right
            let bottomCenterX = lerp(
                topCenterX,
                StickDiagram.x(connection.toLane, size: rect.size),
                t: proportionClosed
            )
            let height = bottomY - topY
            let curveStartDY = height / 5
            return Path {
                if bottomY == rect.maxY {
                    $0.move(to: .init(x: bottomCenterX, y: bottomY))
                    $0.addLine(to: .init(x: bottomCenterX, y: bottomY - curveStartDY))
                }
                if topY == rect.minY {
                    $0.move(to: .init(x: topCenterX, y: topY + curveStartDY))
                    $0.addLine(to: .init(x: topCenterX, y: topY))
                }
            }
        }
    }

    struct NonTwistShape: SwiftUI.Shape {
        var proportionClosed: Double
        let connection: RouteBranchSegment.StickConnection

        var animatableData: Double {
            get { proportionClosed }
            set { proportionClosed = newValue }
        }

        func path(in rect: CGRect) -> Path {
            let openFromVPos = connection.fromVPos == .center ? .bottom : connection.fromVPos
            let openToVPos = connection.toVPos == .center ? .bottom : connection.toVPos
            let opensToNothing = openFromVPos == openToVPos
            if opensToNothing, proportionClosed == 0 { return Path() }
            let topY = lerp(
                StickDiagram.y(openFromVPos, size: rect.size),
                StickDiagram.y(connection.fromVPos, size: rect.size),
                t: proportionClosed
            )
            let bottomY = lerp(
                StickDiagram.y(openToVPos, size: rect.size),
                StickDiagram.y(connection.toVPos, size: rect.size),
                t: proportionClosed
            )
            let centerY = (topY + bottomY) / 2
            let topCenterX = StickDiagram.x(connection.fromLane, size: rect.size)
            // when the twist is untwisted, we want to keep using the from lane so that the segment below the toggle
            // lines up right
            let bottomCenterX = lerp(
                topCenterX,
                StickDiagram.x(connection.toLane, size: rect.size),
                t: proportionClosed
            )
            return Path {
                $0.move(to: .init(x: topCenterX, y: topY))
                $0.addCurve(
                    to: .init(x: bottomCenterX, y: bottomY),
                    control1: .init(x: topCenterX, y: centerY),
                    control2: .init(x: bottomCenterX, y: centerY)
                )
            }
        }
    }

    var body: some View {
        let shadowColor = if #available(iOS 18.0, *) {
            color.mix(with: .black, by: 0.15)
        } else {
            color
        }
        ZStack {
            ForEach(Array(connections.enumerated()), id: \.offset, content: { element in
                let (connection, isTwisted) = element.element
                if isTwisted {
                    let openFromVPos = connection.fromVPos == .center ? .bottom : connection.fromVPos
                    let openToVPos = connection.toVPos == .center ? .bottom : connection.toVPos
                    let opensToNothing = openFromVPos == openToVPos
                    let stickWidth = opensToNothing ? 4 * proportionClosed : 4
                    TwistShadowShape(proportionClosed: proportionClosed, connection: connection)
                        .stroke(shadowColor, style: .init(lineWidth: stickWidth, lineCap: .round))
                    if #unavailable(iOS 18.0) {
                        TwistShadowShape(proportionClosed: proportionClosed, connection: connection)
                            .stroke(.black.opacity(0.15), style: .init(lineWidth: stickWidth, lineCap: .round))
                    }
                    TwistCurvesShape(proportionClosed: proportionClosed, connection: connection)
                        .stroke(color, style: .init(lineWidth: stickWidth, lineCap: .round, lineJoin: .round))
                    TwistEndsShape(proportionClosed: proportionClosed, connection: connection)
                        .stroke(color, style: .init(lineWidth: stickWidth))
                } else {
                    NonTwistShape(proportionClosed: proportionClosed, connection: connection)
                        .stroke(color, lineWidth: 4)
                }
            })
        }
        .frame(width: 40)
    }

    // swiftlint:disable:next identifier_name
    private static func lerp(_ x1: CGFloat, _ x2: CGFloat, t: Double) -> CGFloat {
        x1 * (1 - t) + x2 * t
    }
}

#Preview {
    RouteLineTwist(
        color: .init(hex: "FFC72C"),
        proportionClosed: 1,
        connections: [(
            .init(fromStop: "", toStop: "", fromLane: .center, toLane: .center, fromVPos: .top, toVPos: .bottom),
            true
        )]
    )
    .frame(height: 60)
}

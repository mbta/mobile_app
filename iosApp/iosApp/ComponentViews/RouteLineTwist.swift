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
    let proportionClosed: Float
    let connections: [(RouteBranchSegment.StickConnection, Bool)]

    // according to https://swiftui-lab.com/swiftui-animations-part1/ this is how you get animated paths in SwiftUI

    struct TwistShadowShape: SwiftUI.Shape {
        var proportionClosed: Float
        let connection: RouteBranchSegment.StickConnection

        var animatableData: Float {
            get { proportionClosed }
            set { proportionClosed = newValue }
        }

        func path(in rect: CGRect) -> SwiftUI.Path {
            guard let shape = StickDiagramShapes.shared.twisted(
                connection: connection,
                rect: rect.into(),
                proportionClosed: proportionClosed
            ) else { return .init() }
            return Path {
                $0.move(to: shape.shadow.start.into())
                $0.addLine(to: shape.shadow.end.into())
            }
        }
    }

    struct TwistCurvesShape: SwiftUI.Shape {
        var proportionClosed: Float
        let connection: RouteBranchSegment.StickConnection

        var animatableData: Float {
            get { proportionClosed }
            set { proportionClosed = newValue }
        }

        func path(in rect: CGRect) -> SwiftUI.Path {
            guard let shape = StickDiagramShapes.shared.twisted(
                connection: connection,
                rect: rect.into(),
                proportionClosed: proportionClosed
            ) else { return .init() }
            return Path {
                if let bottom = shape.curves.bottom {
                    $0.move(to: bottom.into())
                    $0.addLine(to: shape.curves.bottomCurveStart.into())
                } else {
                    $0.move(to: shape.curves.bottomCurveStart.into())
                }
                $0.addCurve(
                    to: shape.curves.bottomNearTwist.into(),
                    control1: shape.curves.bottomCurveStartControl.into(),
                    control2: shape.curves.bottomNearTwistControl.into()
                )
                $0.addLine(to: shape.curves.shadowStart.into())
                $0.move(to: shape.curves.shadowEnd.into())
                $0.addLine(to: shape.curves.topNearTwist.into())
                $0.addCurve(
                    to: shape.curves.topCurveStart.into(),
                    control1: shape.curves.topNearTwistControl.into(),
                    control2: shape.curves.topCurveStartControl.into()
                )
                if let top = shape.curves.top {
                    $0.addLine(to: top.into())
                }
            }
        }
    }

    struct TwistEndsShape: SwiftUI.Shape {
        var proportionClosed: Float
        let connection: RouteBranchSegment.StickConnection

        var animatableData: Float {
            get { proportionClosed }
            set { proportionClosed = newValue }
        }

        func path(in rect: CGRect) -> SwiftUI.Path {
            guard let shape = StickDiagramShapes.shared.twisted(
                connection: connection,
                rect: rect.into(),
                proportionClosed: proportionClosed
            ) else { return .init() }
            return Path {
                if let bottom = shape.ends.bottom {
                    $0.move(to: bottom.into())
                    $0.addLine(to: shape.ends.bottomCurveStart.into())
                }
                if let top = shape.ends.top {
                    $0.move(to: shape.ends.topCurveStart.into())
                    $0.addLine(to: top.into())
                }
            }
        }
    }

    struct NonTwistShape: SwiftUI.Shape {
        var proportionClosed: Float
        let connection: RouteBranchSegment.StickConnection

        var animatableData: Float {
            get { proportionClosed }
            set { proportionClosed = newValue }
        }

        func path(in rect: CGRect) -> SwiftUI.Path {
            guard let shape = StickDiagramShapes.shared.nonTwisted(
                connection: connection,
                rect: rect.into(),
                proportionClosed: proportionClosed
            ) else { return .init() }
            return Path {
                $0.move(to: shape.start.into())
                $0.addCurve(
                    to: shape.end.into(),
                    control1: shape.startControl.into(),
                    control2: shape.endControl.into()
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
                    let stickWidth = opensToNothing ? 4 * CGFloat(proportionClosed) : 4
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

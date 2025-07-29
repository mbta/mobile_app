//
//  StickDiagram.swift
//  iosApp
//
//  Created by Melody Horn on 7/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StickDiagram: View {
    var color: Color
    var connections: [RouteBranchSegment.StickConnection]
    var getAlertState: (_ fromStop: String, _ toStop: String) -> SegmentAlertState

    init(
        _ color: Color,
        _ connections: [RouteBranchSegment.StickConnection],
        getAlertState: @escaping (_ fromStop: String, _ toStop: String) -> SegmentAlertState = { _, _ in .normal }
    ) {
        self.color = color
        self.connections = connections
        self.getAlertState = getAlertState
    }

    var body: some View {
        Canvas { context, size in
            for connection in connections {
                let (color, style) = switch getAlertState(connection.fromStop, connection.toStop) {
                case .normal: (color, StrokeStyle(lineWidth: 4))
                case .shuttle: (color, StrokeStyle(lineWidth: 4, dash: [8, 8], dashPhase: 14))
                case .suspension: (Color.deemphasized, StrokeStyle(lineWidth: 4, dash: [8, 8], dashPhase: 14))
                }
                context.stroke(Path {
                    let fromX = Self.x(connection.fromLane, size: size)
                    let toX = Self.x(connection.toLane, size: size)
                    let fromY = Self.y(connection.fromVPos, size: size)
                    let toY = Self.y(connection.toVPos, size: size)
                    let controlY = (fromY + toY) / 2
                    $0.move(to: .init(x: fromX, y: fromY))
                    $0.addCurve(
                        to: .init(x: toX, y: toY),
                        control1: .init(x: fromX, y: controlY),
                        control2: .init(x: toX, y: controlY)
                    )
                }, with: .color(color), style: style)
            }
        }
        .frame(width: 40)
    }

    static func x(_ lane: RouteBranchSegment.Lane, size: CGSize) -> CGFloat {
        switch lane {
        case .left: 10
        case .center: size.width / 2
        case .right: size.width - 10
        }
    }

    static func y(_ vPos: RouteBranchSegment.VPos, size: CGSize) -> CGFloat {
        switch vPos {
        case .top: 0
        case .center: size.height / 2
        case .bottom: size.height
        }
    }
}

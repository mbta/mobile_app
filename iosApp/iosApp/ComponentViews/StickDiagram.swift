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
                let shape = StickDiagramShapes.shared.connection(
                    connection: connection,
                    rect: .init(minX: 0, maxX: Float(size.width), minY: 0, maxY: Float(size.height))
                )
                context.stroke(SwiftUI.Path {
                    $0.move(to: shape.start.into())
                    $0.addCurve(
                        to: shape.end.into(),
                        control1: shape.startControl.into(),
                        control2: shape.endControl.into()
                    )
                }, with: .color(color), style: style)
            }
        }
        .frame(width: 40)
    }
}

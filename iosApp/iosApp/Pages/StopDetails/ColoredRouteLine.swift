//
//  ColoredRouteLine.swift
//  iosApp
//
//  Created by esimon on 12/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct ColoredRouteLine: View {
    var color: Color
    var state: State

    enum State {
        case empty
        case shuttle
        case regular
    }

    init(_ color: Color, state: State = .regular) {
        self.color = color
        self.state = state
    }

    var body: some View {
        Canvas { context, size in
            let style: StrokeStyle? = switch state {
            case .empty: nil
            case .shuttle: .init(lineWidth: size.width, dash: [8, 8], dashPhase: 14)
            case .regular: .init(lineWidth: size.width)
            }
            guard let style else { return }
            context.stroke(Path {
                $0.move(to: .init(x: size.width / 2, y: 0))
                $0.addLine(to: .init(x: size.width / 2, y: size.height))
            }, with: .color(color), style: style)
        }
        .frame(minWidth: 4, maxWidth: 4)
    }
}

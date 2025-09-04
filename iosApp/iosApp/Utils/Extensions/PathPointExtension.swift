//
//  PathPointExtension.swift
//  iosApp
//
//  Created by Melody Horn on 7/29/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreGraphics
import Shared

extension Shared.Path.Point {
    func into() -> CGPoint {
        .init(x: CGFloat(x), y: CGFloat(y))
    }
}

//
//  CGRectExtension.swift
//  iosApp
//
//  Created by Melody Horn on 7/29/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreGraphics
import Shared

extension CGRect {
    func into() -> Shared.Path.Rect {
        .init(minX: Float(minX), maxX: Float(maxX), minY: Float(minY), maxY: Float(maxY))
    }
}

//
//  Minutes.swift
//  iosApp
//
//  Created by esimon on 2/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation

class Minutes {
    init(_ minutes: Int32) {
        self.minutes = minutes
    }

    let minutes: Int32

    var hours: Int32 {
        Int32(
            (Float(minutes) / 60).rounded(FloatingPointRoundingRule.down)
        )
    }

    var remainingMinutes: Int32 {
        minutes - (hours * 60)
    }
}

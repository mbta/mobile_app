//
//  DayOfWeekExtension.swift
//  iosApp
//
//  Created by Melody Horn on 1/22/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared

extension Kotlinx_datetimeDayOfWeek {
    /// An index with Sun..Sat as 0..6.
    var indexSundayFirst: Int { Int(ordinal + 1) % 7 }
}

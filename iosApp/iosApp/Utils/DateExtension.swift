//
//  DateExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 2/29/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared

extension Date {
    init(instant: KotlinInstant) {
        self.init(timeIntervalSince1970: TimeInterval(instant.epochSeconds))
    }
}

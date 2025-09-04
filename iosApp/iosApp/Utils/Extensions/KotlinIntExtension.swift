//
//  KotlinIntExtension.swift
//  iosApp
//
//  Created by Melody Horn on 4/15/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared

extension KotlinInt {
    convenience init?(value: Int?) {
        guard let value else { return nil }
        self.init(int: Int32(value))
    }
}

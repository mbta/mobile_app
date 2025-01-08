//
//  KotlinIntExtension.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-08.
//  Copyright © 2025 MBTA. All rights reserved.
//

import shared

extension KotlinInt {
    convenience init?(int: Int32?) {
        guard let int else { return nil }
        self.init(int: int)
    }
}

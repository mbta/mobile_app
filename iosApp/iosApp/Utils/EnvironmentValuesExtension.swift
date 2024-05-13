//
//  EnvironmentValuesExtension.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-05-13.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

private struct NowEnvironmentKey: EnvironmentKey {
    static let defaultValue: Instant = Date.now.toKotlinInstant()
}

extension EnvironmentValues {
    var now: Instant {
        get { self[NowEnvironmentKey.self] }
        set { self[NowEnvironmentKey.self] = newValue }
    }
}

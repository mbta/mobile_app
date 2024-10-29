//
//  Setting+Convenience.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/11/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

extension Setting: Identifiable {
    var category: MoreSection.Category {
        switch key {
        case .hideMaps: .settings
        case .searchRouteResults: .featureFlags
        case .map: .featureFlags
        }
    }
}

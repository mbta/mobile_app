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
    public var id: UUID {
        UUID()
    }

    var name: String {
        switch key {
        case .searchRouteResults:
            "Search - Route Results"
        case .map:
            "Map Debug"
        }
    }

    var icon: String {
        switch key {
        case .searchRouteResults:
            "point.topleft.down.to.point.bottomright.curvepath.fill"
        case .map:
            "location.magnifyingglass"
        }
    }

    var category: SettingsSection.Category {
        switch key {
        case .searchRouteResults:
            .featureFlags
        case .map:
            .debug
        }
    }
}

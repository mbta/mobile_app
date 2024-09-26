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
        case .search:
            "Search"
        case .searchRouteResults:
            "Search - Route Results"
        case .map:
            "Map Debug"
        case .predictionsV2Channel:
            "Predictions V2 Channel"
        }
    }

    var icon: String {
        switch key {
        case .search:
            "magnifyingglass"
        case .searchRouteResults:
            "point.topleft.down.to.point.bottomright.curvepath.fill"
        case .map:
            "location.magnifyingglass"
        case .predictionsV2Channel:
            "magnifyingglass"
        }
    }

    var category: SettingsSection.Category {
        switch key {
        case .search:
            .featureFlags
        case .searchRouteResults:
            .featureFlags
        case .predictionsV2Channel:
            .featureFlags
        case .map:
            .debug
        }
    }
}

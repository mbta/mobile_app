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
        case .map:
            "Map Debug"
        }
    }

    var icon: String {
        switch key {
        case .search:
            "magnifyingglass"
        case .map:
            "location.magnifyingglass"
        }
    }

    var category: SettingsSection.Category {
        switch key {
        case .search:
            .featureFlags
        case .map:
            .debug
        }
    }
}

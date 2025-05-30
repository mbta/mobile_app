//
//  SelectedTab.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 1/23/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import DeveloperToolsSupport
import Foundation

enum SelectedTab: Hashable, CaseIterable {
    case favorites
    case nearby
    case more

    var imageResource: ImageResource {
        switch self {
        case .favorites:
            .tabIconFavorites
        case .nearby:
            .tabIconNearby
        case .more:
            .tabIconMore
        }
    }

    var text: String {
        switch self {
        case .favorites: NSLocalizedString(
                "Favorites",
                comment: "The label for the Favorites page in the navigation bar"
            )
        case .nearby: NSLocalizedString(
                "Nearby",
                comment: "The label for the Nearby Transit page in the navigation bar"
            )
        case .more: NSLocalizedString(
                "More",
                comment: "The label for the More page in the navigation bar"
            )
        }
    }

    var associatedSheetNavEntry: SheetNavigationStackEntry {
        switch self {
        case .favorites: .favorites
        case .nearby: .nearby
        case .more: .more
        }
    }
}

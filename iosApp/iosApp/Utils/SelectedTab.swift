//
//  SelectedTab.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 1/23/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import DeveloperToolsSupport
import Foundation
import Shared

enum SelectedTab: Hashable {
    case favorites
    case nearby
    case more(highlight: MoreSection.Category?)

    static func == (lhs: SelectedTab, rhs: SelectedTab) -> Bool {
        lhs.hashValue == rhs.hashValue
    }

    // Implement hashable to ignore the highlighted more category
    func hash(into hasher: inout Hasher) {
        let hashString = switch self {
        case .favorites: "favorites"
        case .nearby: "nearby"
        case .more: "more"
        }
        hasher.combine(hashString)
    }

    var imageResource: ImageResource {
        switch self {
        case .favorites: .tabIconFavorites
        case .nearby: .tabIconNearby
        case .more: .tabIconMore
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
        case let .more(category): .more(highlight: category)
        }
    }
}

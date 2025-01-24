//
//  SelectedTab.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 1/23/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation

enum SelectedTab: Hashable, CaseIterable {
    case nearby
    case more

    var imageResource: ImageResource {
        switch self {
        case .nearby:
            .tabIconNearby
        case .more:
            .tabIconMore
        }
    }

    var text: String {
        switch self {
        case .nearby: NSLocalizedString(
                "Nearby",
                comment: "The label for the Nearby Transit page in the navigation bar"
            )
        case .more: NSLocalizedString("More", comment: "The label for the More page in the navigation bar")
        }
    }
}
